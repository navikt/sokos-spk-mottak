package no.nav.sokos.spk.mottak.service

import com.ibm.db2.jcc.am.BatchUpdateException
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
import no.nav.sokos.spk.mottak.util.MQ_BATCH_SIZE

private val logger = KotlinLogging.logger { }

class SendTrekkTransaksjonToOppdragServiceV2(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource),
    private val outboxRepository: OutboxRepository = OutboxRepository(),
) {
    fun getTrekkTransaksjonAndSendToOppdrag() {
        val transaksjoner = getTransaksjoner() ?: return
        Metrics.timer(SERVICE_CALL, "SendTrekkTransaksjonToOppdragServiceV2", "getTrekkTransaksjonAndSendToOppdrag").recordCallable {
            logger.info { "Starter sending av ${transaksjoner.size} trekktransaksjoner til OppdragZ" }
            val totalTransaksjoner = processTransaksjoner(transaksjoner)
            Metrics.trekkTransaksjonerTilOppdragCounter.inc(totalTransaksjoner.toLong())
        }
    }

    private fun getTransaksjoner(): List<Transaksjon>? {
        return runCatching {
            transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
        }.onFailure { exception ->
            logger.error(exception) { "Fatal feil ved henting av trekktransaksjoner: ${exception.message}" }
            throw RuntimeException("Fatal feil ved henting av trekktransaksjoner")
        }.getOrNull()
    }

    private fun processTransaksjoner(transaksjoner: List<Transaksjon>): Int {
        var totalTransaksjoner = 0
        transaksjoner.chunked(MQ_BATCH_SIZE).forEach { chunk ->
            val transaksjonIdList = chunk.mapNotNull { it.transaksjonId }
            runCatching {
                dataSource.transaction { session ->

                    val trekkMeldinger = chunk.map { JaxbUtils.marshallTrekk(it.innrapporteringTrekk()) }
                    updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_OK, trekkMeldinger, session)
                    logger.info { "Inserted ${trekkMeldinger.size} trekkmeldinger" }
                    totalTransaksjoner += transaksjonIdList.size
                }
            }.onFailure { exception ->
                logger.error(exception) { "Feiler ved utsending av trekktransaksjonene: ${transaksjonIdList.joinToString()} : $exception" }
                if (exception is BatchUpdateException) {
                    val ex: BatchUpdateException = exception
                    logger.error { "Feilårsak er ${ex.nextException}" }
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
        val transaksjonTilstandIdList = transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session = session)
        transaksjonRepository.updateBatch(transaksjonIdList, transaksjonTilstandIdList, transTilstandStatus, session = session)
        outboxRepository.insertTrekk(transaksjonIdList.zip(trekkMeldinger).toMap(), session = session)
    }
}
