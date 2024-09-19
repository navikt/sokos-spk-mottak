package no.nav.sokos.spk.mottak.service

import com.ibm.db2.jcc.am.BatchUpdateException
import com.zaxxer.hikari.HikariDataSource
import jakarta.xml.bind.JAXBElement
import kotliquery.Session
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdrag110
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdragsLinje150
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.metrics.SERVICE_CALL
import no.nav.sokos.spk.mottak.repository.OutboxRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import no.nav.sokos.spk.mottak.util.XmlUtils.parseXmlToString
import no.nav.sokos.spk.mottak.util.XmlUtils.parseXmlToStringList
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.time.Instant

private val logger = KotlinLogging.logger { }
private const val BATCH_SIZE = 1000

class SendUtbetalingTransaksjonToOppdragServiceV2(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource),
    private val outboxRepository: OutboxRepository = OutboxRepository(),
) {
    fun fetchUtbetalingTransaksjonAndSendToOppdrag() {
        val timer = Instant.now()
        var totalTransaksjoner = 0

        Metrics.timer(SERVICE_CALL, "SendUtbetalingTransaksjonToOppdragServiceV2", "fetchUtbetalingTransaksjonAndSendToOppdrag").recordCallable {
            runCatching {
                val transaksjonList = transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
                if (transaksjonList.isNotEmpty()) {
                    logger.info { "Starter sending av ${transaksjonList.size} utbetalingstransaksjoner til OppdragZ" }
                    val oppdragList =
                        transaksjonList
                            .groupBy { Pair(it.personId, it.gyldigKombinasjon!!.fagomrade) }
                            .map { it.value.toOppdrag() }
                            .map { JaxbUtils.marshallOppdrag(it) }
                    logger.info { "Oppdragslistestørrelse ${oppdragList.size}" }
                    oppdragList.chunked(BATCH_SIZE).forEach { oppdragChunk ->
                        logger.info { "Sender ${oppdragChunk.size} utbetalingsmeldinger " }
                        sendToOppdrag(oppdragChunk)
                        totalTransaksjoner += oppdragChunk.size
                    }
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

    private fun sendToOppdrag(oppdragList: List<String>) {
        dataSource.transaction { session ->
            var transaksjonIdList = emptyList<Int>()
            runCatching {
                transaksjonIdList = findTransaksjonIdList(oppdragList)
                val messageKeyList = findMessageKeyList(oppdragList)
                updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_OK, messageKeyList, oppdragList, session)
                logger.info { "Inserted ${oppdragList.size} utbetalingsmeldinger" }
            }.onFailure { exception ->
                logger.error(exception) { "Feiler ved utsending av ${transaksjonIdList.size} utbetalingstransaksjoner: $exception" }
                if (exception is BatchUpdateException) {
                    val ex: BatchUpdateException = exception
                    logger.error { "Feilårsak er ${ex.nextException}" }
                }
            }
        }
    }

    private fun findTransaksjonIdList(oppdragList: List<String>): List<Int> = oppdragList.flatMap { parseXmlToStringList(it, "delytelseId").map { it.toInt() } }

    private fun findMessageKeyList(oppdragList: List<String>): List<String> =
        oppdragList.map {
            parseXmlToString(it, "fagsystemId") + parseXmlToString(it, "kodeFagomraade") + parseXmlToString(it, "nokkelAvstemming")
        }

    private fun updateTransaksjonAndTransaksjonTilstand(
        transaksjonIdList: List<Int>,
        transTilstandStatus: String,
        messageKeyList: List<String> = emptyList(),
        utbetalingsMeldinger: List<String> = emptyList(),
        session: Session,
    ) {
        transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, transTilstandStatus, session = session)
        val transaksjonTilstandIdList = transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session = session)
        transaksjonRepository.updateTransTilstand(transaksjonIdList, transaksjonTilstandIdList, session = session)
        outboxRepository.insertUtbetaling(messageKeyList.zip(utbetalingsMeldinger).toMap(), session = session)
    }
}
