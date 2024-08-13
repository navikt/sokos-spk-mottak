package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
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
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }
private const val BATCH_SIZE = 100

class SendUtbetalingTransaksjonTilOppdragService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val producer: JmsProducerService = JmsProducerService(),
) {
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    fun hentUtbetalingTransaksjonOgSendTilOppdrag() {
        val timer = Instant.now()
        var totalTransaksjoner = 0

        Metrics.timer(SERVICE_CALL, "SendUtbetalingTransaksjonTilOppdragService", "hentUtbetalingTransaksjonOgSendTilOppdrag").recordCallable {
            runCatching {
                val transaksjonList = transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
                if (transaksjonList.isNotEmpty()) {
                    logger.info { "Starter sending av utbetalingstransaksjoner til OppdragZ" }

                    transaksjonList.chunked(BATCH_SIZE).forEach { items ->
                        val oppdragList =
                            transaksjonList
                                .groupBy { Pair(it.personId, it.gyldigKombinasjon!!.fagomrade) }
                                .map { it.value.toOppdrag() }
                                .map { JaxbUtils.marshallOppdrag(it) }

                        val transaksjonIdList = items.map { it.transaksjonId!! }

                        sendTilOppdrag(oppdragList, transaksjonIdList)
                        totalTransaksjoner += oppdragList.size
                    }
                    logger.info { "$totalTransaksjoner utbetalingstransaksjoner sendt til OppdragZ på ${Duration.between(timer, Instant.now()).toSeconds()} sekunder. " }
                    Metrics.utbetalingTransaksjonerTilOppdragCounter.inc(totalTransaksjoner.toLong())
                }
            }.onFailure { exception ->
                val errorMessage = "Feil under sending av utbetalingstransaksjoner til OppdragZ. Feilmelding: ${exception.message}"
                logger.error(exception) { errorMessage }
                throw MottakException(errorMessage)
            }
        }
    }

    private fun List<Transaksjon>.toOppdrag(): JAXBElement<Oppdrag> =
        ObjectFactory().createOppdrag(
            Oppdrag().apply {
                oppdrag110 =
                    first().oppdrag110().apply {
                        oppdragsLinje150.addAll(map { it.oppdragsLinje150() })
                    }
            },
        )

    private fun sendTilOppdrag(
        oppdragList: List<String>,
        transaksjonIdList: List<Int>,
    ) {
        dataSource.transaction { session ->
            val transaksjonTilstandIdList = mutableListOf<Long>()
            runCatching {
                transaksjonTilstandIdList.addAll(updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_OK, session))
                producer.send(
                    oppdragList,
                    MQQueue(PropertiesConfig.MQProperties().utbetalingQueueName),
                    MQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                )

                logger.debug { "TransaksjonsId: ${transaksjonIdList.joinToString()} er sendt til OppdragZ." }
            }.onFailure { exception ->
                transaksjonTilstandRepository.deleteTransaksjon(transaksjonTilstandIdList, session)
                updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_FEIL, session)
                logger.error(exception) { "TransaksjonsId: ${transaksjonIdList.joinToString()} blir ikke sendt til OppdragZ." }
            }
        }
    }

    private fun updateTransaksjonAndTransaksjonTilstand(
        transaksjonIdList: List<Int>,
        transTilstandStatus: String,
        session: Session,
    ): List<Long> {
        transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, transTilstandStatus, session)
        return transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session)
    }
}
