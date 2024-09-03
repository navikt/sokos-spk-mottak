package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.converter.TrekkConverter.innrapporteringTrekk
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.metrics.SERVICE_CALL
import no.nav.sokos.spk.mottak.repository.OutboxRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils

private val logger = KotlinLogging.logger { }
private const val BATCH_SIZE = 100

class SendTrekkTransaksjonTilOppdragService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource),
    private val outboxRepository: OutboxRepository = OutboxRepository(dataSource),
) {
    fun hentTrekkTransaksjonOgSendTilOppdrag() {
        val transaksjoner = fetchTransaksjoner() ?: return
        Metrics.timer(SERVICE_CALL, "SendTrekkTransaksjonTilOppdragService", "hentTrekkTransaksjonOgSendTilOppdrag").recordCallable {
            logger.info { "Starter sending av trekktransaksjoner til OppdragZ" }
            val totalTransaksjoner = processTransaksjoner(transaksjoner)
            Metrics.trekkTransaksjonerTilOppdragCounter.inc(totalTransaksjoner.toLong())
        }
    }

    private fun fetchTransaksjoner(): List<Transaksjon>? {
        return runCatching {
            transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
        }.onFailure { exception ->
            logger.error(exception) { "Fatal feil ved henting av trekktransaksjoner: ${exception.message}" }
            throw RuntimeException("Fatal feil ved henting av trekktransaksjoner")
        }.getOrNull()
    }

    private fun processTransaksjoner(transaksjoner: List<Transaksjon>): Int {
        var totalTransaksjoner = 0
        transaksjoner.chunked(BATCH_SIZE).forEach { chunk ->
            val transaksjonIdList = chunk.mapNotNull { it.transaksjonId }
            dataSource.transaction { session ->
                runCatching {
                    val trekkMeldinger = chunk.map { JaxbUtils.marshallTrekk(it.innrapporteringTrekk()) }
                    updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_OK, trekkMeldinger, session)
                    totalTransaksjoner += transaksjonIdList.size
                }.onFailure { exception ->
                    logger.error(exception) { "Trekktransaksjoner: ${transaksjonIdList.minOrNull()} - ${transaksjonIdList.maxOrNull()} feiler ved sending til OppdragZ: ${exception.message}" }
                }
            }
        }
        return totalTransaksjoner
    }

    private fun updateTransaksjonOgTransaksjonTilstand(
        transaksjonIdList: List<Int>,
        transTilstandStatus: String,
        trekkMeldinger: List<String> = emptyList(),
        session: Session,
    ) {
        transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, transTilstandStatus, session = session)
        val transaksjonTilstandIdList = transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session = session)
        transaksjonRepository.updateTransTilstand(transaksjonIdList, transaksjonTilstandIdList, session = session)
        outboxRepository.insert(transaksjonIdList.zip(trekkMeldinger).toMap(), session = session)
    }
}
