package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
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
import no.nav.sokos.spk.mottak.domain.converter.TrekkConverter.innrapporteringTrekk
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }
private const val BATCH_SIZE = 100

class SendTrekkTransaksjonTilOppdragService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val producer: JmsProducerService = JmsProducerService(),
) {
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    fun hentTrekkTransaksjonOgSendTilOppdrag() {
        val timer = Instant.now()
        var totalTransaksjoner = 0
        val transaksjoner =
            runCatching {
                transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
            }.getOrElse { exception ->
                val errorMessage = "Feil under henting av trekktransaksjoner. Feilmelding: ${exception.message}"
                logger.error(exception) { errorMessage }
                throw MottakException(errorMessage)
            }
        if (transaksjoner.isEmpty()) return

        logger.info { "Starter sending av trekktransaksjoner til OppdragZ" }
        transaksjoner.chunked(BATCH_SIZE).forEach { chunk ->
            val transaksjonIdList = chunk.mapNotNull { it.transaksjonId }
            val transaksjonTilstandIdList =
                using(sessionOf(dataSource)) { session ->
                    updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_OK)
                }

            val trekkMeldinger = chunk.map { JaxbUtils.marshallTrekk(it.innrapporteringTrekk()) }
            runCatching {
                producer.send(
                    trekkMeldinger,
                    MQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                    MQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                )
                totalTransaksjoner += transaksjonIdList.size
                logger.info { "$totalTransaksjoner trekktransaksjoner sendt til OppdragZ brukte ${Duration.between(timer, Instant.now()).toSeconds()} sekunder. " }
                Metrics.counter("trekkTransaksjonerTilOppdrag", "Counts the number of trekk transactions sent to OppdragZ").inc(totalTransaksjoner.toLong())
            }.onFailure { exception ->
                transaksjonTilstandRepository.deleteTransaksjon(transaksjonTilstandIdList, sessionOf(dataSource))
                updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_FEIL)
                logger.error(exception) { "Trekktransaksjoner: ${transaksjonIdList.minOrNull()} - ${transaksjonIdList.maxOrNull()} feiler ved sending til OppdragZ. " }
            }
        }
    }

    private fun updateTransaksjonOgTransaksjonTilstand(
        transaksjonIdList: List<Int>,
        transTilstandStatus: String,
    ): List<Long> =
        using(sessionOf(dataSource)) { session ->
            transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, transTilstandStatus, session)
            transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session)
        }
}
