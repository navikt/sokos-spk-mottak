package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import java.time.Instant
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.isTransaksjonStatusOK
import no.nav.sokos.spk.mottak.domain.toTransaksjon
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.repository.AvvikTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository

private val logger = KotlinLogging.logger {}

class ValidateTransaksjonService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource)
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)
    private val avvikTransaksjonRepository: AvvikTransaksjonRepository = AvvikTransaksjonRepository(dataSource)

    fun validateInnTransaksjon() {
        logger.info { "Transaksjonsvalidering jobben startet" }
        val timer = Instant.now()
        var totalInnTransaksjoner = 0
        var totalAvvikTransaksjoner = 0

        runCatching {
            executeInntransaksjonValidation()

            while (true) {
                val innTransaksjonList = innTransaksjonRepository.getByBehandletWithPersonId()
                totalInnTransaksjoner += innTransaksjonList.size
                totalAvvikTransaksjoner += innTransaksjonList.filter { !it.isTransaksjonStatusOK() }.size
                logger.debug { "Henter inn ${innTransaksjonList.size} innTransaksjoner fra databasen" }

                when {
                    innTransaksjonList.isNotEmpty() -> saveTransaksjonAndAvvikTransaksjon(innTransaksjonList)
                    else -> break
                }
            }
            when {
                totalInnTransaksjoner > 0 -> logger.info {
                    "$totalInnTransaksjoner innTransaksjoner validert på ${Duration.between(timer, Instant.now()).toSeconds()} sekunder. " +
                            "Opprettet ${totalInnTransaksjoner.minus(totalAvvikTransaksjoner)} transaksjoner og $totalAvvikTransaksjoner avvikstransaksjoner "
                }

                else -> logger.info { "Finner ingen innTransaksjoner som er ubehandlet" }
            }
        }.onFailure { exception ->
            logger.error(exception) { "Feil under behandling av innTransaksjoner" }
            throw MottakException("Feil under behandling av innTransaksjoner")
        }
    }

    private fun saveTransaksjonAndAvvikTransaksjon(innTransaksjonList: List<InnTransaksjon>) {
        val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }

        dataSource.transaction { session ->
            innTransaksjonMap[true]?.takeIf { it.isNotEmpty() }?.apply {
                val transaksjonMap = transaksjonRepository.getLastTransaksjonByPersonId(this.map { it.personId!! }).associateBy { it.personId }
                transaksjonRepository.insertBatch(this.map { it.toTransaksjon(transaksjonMap[it.personId]) }, session)

                val transaksjonIdList = this.map { it.innTransaksjonId!! }
                transaksjonTilstandRepository.insertBatch(transaksjonIdList, session)

                logger.debug { "${transaksjonIdList.size} transaksjoner opprettet" }
            }

            innTransaksjonMap[false]?.takeIf { it.isNotEmpty() }?.apply {
                val avvikTransaksjonIdList = avvikTransaksjonRepository.insertBatch(this, session)
                logger.debug { "${avvikTransaksjonIdList.size} avvik transaksjoner opprettet: " }
            }

            innTransaksjonRepository.updateBehandletStatusBatch(innTransaksjonList.map { it.innTransaksjonId!! }, session)
        }
    }

    private fun executeInntransaksjonValidation() {
        dataSource.transaction { session ->
            innTransaksjonRepository.validateTransaksjon(session)
        }
    }
}