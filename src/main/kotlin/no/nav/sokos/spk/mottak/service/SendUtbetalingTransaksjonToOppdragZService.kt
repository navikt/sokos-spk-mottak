package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import jakarta.xml.bind.JAXBElement
import kotliquery.Session
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
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
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import no.nav.sokos.spk.mottak.util.MQ_BATCH_SIZE
import no.nav.sokos.spk.mottak.util.XmlUtils
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

class SendUtbetalingTransaksjonToOppdragZService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource),
    private val filInfoRepository: FilInfoRepository = FilInfoRepository(dataSource),
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
    fun getUtbetalingTransaksjonAndSendToOppdragZ() {
        val timer = Instant.now()
        var totalTransaksjoner = 0

        Metrics.timer(SERVICE_CALL, "SendUtbetalingTransaksjonToOppdragServiceV2", "getUtbetalingTransaksjonAndSendToOppdragZ").recordCallable {
            runCatching {
                val transaksjonList = transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
                if (transaksjonList.isNotEmpty()) {
                    logger.info { "Starter sending av ${transaksjonList.size} utbetalingstransaksjoner til OppdragZ" }
                    val oppdragList =
                        transaksjonList
                            .groupBy { Pair(it.personId, it.gyldigKombinasjon!!.fagomrade) }
                            .map { it.value.toUtbetalingsOppdrag() }
                            .map { JaxbUtils.marshallOppdrag(it) }
                    logger.info { "Oppdragslistestørrelse ${oppdragList.size}" }
                    oppdragList.chunked(MQ_BATCH_SIZE).forEach { oppdragChunk ->
                        logger.info { "Sender ${oppdragChunk.size} utbetalingsmeldinger " }
                        sendToOppdragZ(oppdragChunk)
                        totalTransaksjoner += oppdragChunk.size
                    }
                    logger.info { "Fullført sending av $totalTransaksjoner utbetalingstransaksjoner til OppdragZ. Total tid: ${Duration.between(timer, Instant.now()).toSeconds()} sekunder." }
                    Metrics.utbetalingTransaksjonerTilOppdragCounter.inc(totalTransaksjoner.toLong())

                    dataSource.transaction { session ->
                        filInfoRepository.updateAvstemmingStatus(transaksjonList.distinctBy { it.filInfoId }.map { it.filInfoId }, TRANS_TILSTAND_OPPDRAG_SENDT_OK, session)
                    }
                }
            }.onFailure { exception ->
                val errorMessage = "Feil under sending av utbetalingstransaksjoner til OppdragZ. Feilmelding: ${exception.message}"
                logger.error(exception) { errorMessage }
                throw MottakException(errorMessage)
            }
        }
    }

    private fun List<Transaksjon>.toUtbetalingsOppdrag(): JAXBElement<Oppdrag> =
        ObjectFactory().createOppdrag(
            Oppdrag().apply {
                oppdrag110 =
                    first().oppdrag110().apply {
                        oppdragsLinje150.addAll(map { it.oppdragsLinje150() })
                    }
            },
        )

    private fun sendToOppdragZ(oppdragList: List<String>) {
        dataSource.transaction { session ->
            var transaksjonIdList = emptyList<Int>()
            runCatching {
                producer.send(oppdragList)
                transaksjonIdList = findTransaksjonIdList(oppdragList)
                updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_OK, session)
            }.onFailure { exception ->
                if (exception is MottakException) {
                    runCatching {
                        updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_FEIL, session)
                    }.onFailure { exception ->
                        logger.error(exception) { "DB2-feil: $exception" }
                    }
                }
                logger.error(exception) { "Feiler ved utsending av utbetalingstransaksjonene: ${transaksjonIdList.joinToString()} : $exception" }
            }
        }
    }

    private fun findTransaksjonIdList(oppdragList: List<String>): List<Int> = oppdragList.flatMap { XmlUtils.parseXmlToStringList(it, "delytelseId").map { it.toInt() } }

    private fun updateTransaksjonAndTransaksjonTilstand(
        transaksjonIdList: List<Int>,
        transTilstandStatus: String,
        session: Session,
    ) {
        val transTilstandIdList = transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session = session)
        transaksjonRepository.updateBatch(transaksjonIdList, transTilstandIdList, transTilstandStatus, session = session)
    }
}
