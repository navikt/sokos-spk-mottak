package no.nav.sokos.spk.mottak.service

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.util.concurrent.atomic.AtomicInteger

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import jakarta.xml.bind.JAXBElement
import kotliquery.Session
import mu.KotlinLogging
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQ_BATCH_SIZE
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdrag110
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdragsLinje150
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.metrics.SERVICE_CALL
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.repository.FilInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

private val logger = KotlinLogging.logger { }

class SendUtbetalingTransaksjonToOppdragZService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource),
    private val filInfoRepository: FilInfoRepository = FilInfoRepository(dataSource),
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource),
    private val mqBatchSize: Int = MQ_BATCH_SIZE,
    private val producer: JmsProducerService =
        JmsProducerService(
            MQQueue(PropertiesConfig.MQProperties().utbetalingQueueName).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            },
            MQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            },
            Metrics.mqUtbetalingProducerMetricCounter,
        ),
) {
    private val errorCounter = AtomicInteger(0)

    fun getUtbetalingTransaksjonAndSendToOppdragZ() {
        errorCounter.set(0)

        if (innTransaksjonRepository.countByInnTransaksjon() > 0) {
            logger.info { "Eksisterer innTransaksjoner som ikke er ferdig behandlet og derfor blir ingen utbetalingstransaksjoner behandlet" }
            return
        }

        val timer = Instant.now()
        var transaksjonerSendt = 0
        var oppdragsmeldingerSendt = 0

        Metrics.timer(SERVICE_CALL, "SendUtbetalingTransaksjonToOppdragServiceV2", "getUtbetalingTransaksjonAndSendToOppdragZ").recordCallable {
            runCatching {
                val transaksjonMap =
                    transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
                        .groupBy { it.filInfoId }
                        .toSortedMap()

                transaksjonMap.forEach { (filInfoId, transaksjonList) ->
                    logger.info { "Starter sending fileInfoId: $filInfoId av ${transaksjonList.size} utbetalingstransaksjoner til OppdragZ" }
                    if (transaksjonList.isNotEmpty()) {
                        val oppdragList =
                            transaksjonList
                                .groupBy { Pair(it.personId, it.gyldigKombinasjon!!.fagomrade) }
                                .map { it.value.toUtbetalingsOppdragXML() }
                        logger.debug { "Oppdragslistestørrelse ${oppdragList.size}" }

                        oppdragList.chunked(mqBatchSize).forEach { oppdragChunk ->
                            val transaksjonIdList =
                                oppdragChunk.flatMap { oppdrag ->
                                    oppdrag.value.oppdrag110.oppdragsLinje150
                                        .map { it.delytelseId.toInt() }
                                }
                            val oppdragXML = oppdragChunk.map { JaxbUtils.marshallOppdrag(it) }

                            sendToOppdragZ(oppdragXML, transaksjonIdList)
                            oppdragsmeldingerSendt += oppdragXML.size
                            transaksjonerSendt += transaksjonIdList.size
                            logger.info { "$oppdragsmeldingerSendt utbetalingsmeldinger sendt av totalt ${oppdragList.size} ($transaksjonerSendt transaksjoner av totalt ${transaksjonList.size}) " }
                        }
                        logger.info {
                            "Fullført sending av $oppdragsmeldingerSendt utbetalingsmeldinger ($transaksjonerSendt transaksjoner) til OppdragZ. Total tid: ${
                                Duration.between(
                                    timer,
                                    Instant.now(),
                                ).toSeconds()
                            } sekunder."
                        }
                        Metrics.utbetalingTransaksjonerTilOppdragCounter.inc(transaksjonerSendt.toLong())

                        if (errorCounter.get() == 0) {
                            dataSource.transaction { session ->
                                filInfoRepository.updateAvstemmingStatus(transaksjonList.distinctBy { it.filInfoId }.map { it.filInfoId }, TRANS_TILSTAND_OPPDRAG_SENDT_OK, LocalDate.now(), session)
                            }
                        }
                    }
                }
            }.onFailure { exception ->
                val errorMessage = "Sending av utbetalingstransaksjoner til OppdragZ feilet. Feilmelding: ${exception.message}"
                logger.error(exception) { errorMessage }
                throw MottakException(errorMessage)
            }
        }
    }

    private fun List<Transaksjon>.toUtbetalingsOppdragXML(): JAXBElement<Oppdrag> =
        ObjectFactory().createOppdrag(
            Oppdrag().apply {
                oppdrag110 =
                    first().oppdrag110().apply {
                        oppdragsLinje150.addAll(map { it.oppdragsLinje150() })
                    }
            },
        )

    private fun sendToOppdragZ(
        oppdragList: List<String>,
        transaksjonIdList: List<Int>,
    ) {
        runCatching {
            dataSource.transaction { session ->
                producer.send(oppdragList)
                updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_OK, session)
            }
        }.onFailure { exception ->
            if (exception is MottakException) {
                runCatching {
                    errorCounter.incrementAndGet()
                    dataSource.transaction { session ->
                        updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_FEIL, session)
                    }
                }.onFailure { databaseException ->
                    logger.error(databaseException) { "DB2-feil: ${databaseException.message}" }
                }
            }
            logger.error(exception) { "Utsending av utbetalingstransaksjonene: ${transaksjonIdList.joinToString()} feilet. $exception" }
        }
    }

    private fun updateTransaksjonAndTransaksjonTilstand(
        transaksjonIdList: List<Int>,
        transTilstandStatus: String,
        session: Session,
    ) {
        val transTilstandIdList = transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session = session)
        transaksjonRepository.updateBatch(transaksjonIdList, transTilstandIdList, transTilstandStatus, session = session)
    }
}
