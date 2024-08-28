package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.converter.TrekkConverter.innrapporteringTrekk
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.metrics.SERVICE_CALL
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import java.sql.SQLException
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }
private const val BATCH_SIZE = 100

class SendTrekkTransaksjonTilOppdragService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource),
    private val producer: JmsProducerService =
        JmsProducerService(
            MQQueue(PropertiesConfig.MQProperties().trekkQueueName).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            },
            MQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            },
            Metrics.mqTrekkProducerMetricCounter,
        ),
) {
    fun hentTrekkTransaksjonOgSendTilOppdrag() {
        val timer = Instant.now()
        val transaksjoner = fetchTransaksjoner() ?: return

        Metrics.timer(SERVICE_CALL, "SendTrekkTransaksjonTilOppdragService", "hentTrekkTransaksjonOgSendTilOppdrag").recordCallable {
            logger.info { "Starter sending av trekktransaksjoner til OppdragZ" }
            val totalTransaksjoner = processTransaksjoner(transaksjoner)
            logger.info { "Fullført sending av $totalTransaksjoner trekktransaksjoner til OppdragZ. Total tid: ${Duration.between(timer, Instant.now()).toSeconds()} sekunder." }
            Metrics.trekkTransaksjonerTilOppdragCounter.inc(totalTransaksjoner.toLong())
        }
    }

    private fun fetchTransaksjoner(): List<Transaksjon>? {
        return runCatching {
            transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
        }.onFailure { exception ->
            logger.error(exception) { "Feil under henting av trekktransaksjoner. Feilmelding: ${exception.message}" }
            throw MottakException("Feil under henting av trekktransaksjoner. Feilmelding: ${exception.message}")
        }.getOrNull()
    }

    private fun processTransaksjoner(transaksjoner: List<Transaksjon>): Int {
        var totalTransaksjoner = 0
        val transaksjonTilstandIdList = mutableListOf<Int>()
        transaksjoner.chunked(BATCH_SIZE).forEach { chunk ->
            val transaksjonIdList = chunk.mapNotNull { it.transaksjonId }
            runCatching {
                val trekkMeldinger = chunk.map { JaxbUtils.marshallTrekk(it.innrapporteringTrekk()) }
                val transaksjonTilstandIdList = updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_OK)
                producer.send(trekkMeldinger)
                totalTransaksjoner += transaksjonIdList.size
            }.onFailure { exception ->
                handleException(exception, transaksjonIdList, transaksjonTilstandIdList)
            }
        }
        return totalTransaksjoner
    }

    private fun handleException(
        exception: Throwable,
        transaksjonIdList: List<Int>,
        transaksjonTilstandIdList: List<Int>,
    ) {
        when (exception) {
            is MottakException -> { //  MQ-feil
                logger.error { "MottakException : $exception" }
                transaksjonTilstandRepository.deleteTransaksjon(transaksjonTilstandIdList, sessionOf(dataSource))
                updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_FEIL)
            }

            is SQLException -> { //  DB-feil
                logger.error { "SQLException : $exception" }
                updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_FEIL)
            }

            else -> {
                logger.error { "Exception : $exception" }
                transaksjonTilstandRepository.deleteTransaksjon(transaksjonTilstandIdList, sessionOf(dataSource))
                updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_FEIL)
            }
        }
        logger.error(exception) { "Trekktransaksjoner: ${transaksjonIdList.minOrNull()} - ${transaksjonIdList.maxOrNull()} feiler ved sending til OppdragZ: ${exception.message}" }
    }

    private fun updateTransaksjonOgTransaksjonTilstand(
        transaksjonIdList: List<Int>,
        transTilstandStatus: String,
    ): List<Int> {
        return runCatching {
            using(sessionOf(dataSource)) { session ->
                transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, transTilstandStatus, session = session)
                transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session = session)
            }
        }.onFailure { exception ->
            logger.error { "transaksjonIdList: $transaksjonIdList, transTilstandStatus: $transTilstandStatus" }
            throw SQLException("Feil under henting av trekktransaksjoner. Feilmelding: ${exception.message}")
        }.getOrNull() ?: emptyList()
    }
}
