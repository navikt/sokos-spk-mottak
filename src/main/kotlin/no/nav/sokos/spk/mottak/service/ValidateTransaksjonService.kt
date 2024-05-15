package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
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
import java.time.Duration
import java.time.Instant

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
            if (innTransaksjonRepository.getByBehandletWithPersonId(rows = 1).isNotEmpty()) {
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
                if (transaksjonRepository.getAllFnrWhereTranstolkningIsNyForMoreThanOneInstance().isNotEmpty()) {
                    logger.info { "Det finnes flere inn-transaksjoner med samme fnr og transTolkning = 'NY'" }
                    checkTranstolkning(transaksjonRepository.getAllFnrWhereTranstolkningIsNyForMoreThanOneInstance())
                }
                when {
                    totalInnTransaksjoner > 0 ->
                        logger.info {
                            "$totalInnTransaksjoner innTransaksjoner validert på ${
                                Duration.between(timer, Instant.now()).toSeconds()
                            } sekunder. " +
                                "Opprettet ${totalInnTransaksjoner.minus(totalAvvikTransaksjoner)} transaksjoner og $totalAvvikTransaksjoner avvikstransaksjoner "
                        }

                    else -> logger.info { "Finner ingen innTransaksjoner som er ubehandlet" }
                }
            }
        }.onFailure { exception ->
            logger.error(exception) { "Feil under behandling av innTransaksjoner" }
            throw MottakException("Feil under behandling av innTransaksjoner")
        }
    }

    private fun checkTranstolkning(fnrWithTranstolkningNew: List<String>) {
        for (fnr in fnrWithTranstolkningNew) {
            val (fagomraadeList, artList) = transaksjonRepository.getAllFagomraadeAndArtForFnr(fnr).unzip()
            val uniqueFagomraadeSet = mutableSetOf<String>()
            for (i in fagomraadeList.indices) {
                if (fagomraadeList[i] in uniqueFagomraadeSet) {
                    transaksjonRepository.updateTransTolkningForFnr(fnr, artList[i])
                } else {
                    uniqueFagomraadeSet.add(fagomraadeList[i])
                    logger.info { "Fagomraade endret for fnr: $fnr" }
                }
            }
        }
    }

    private fun saveTransaksjonAndAvvikTransaksjon(innTransaksjonList: List<InnTransaksjon>) {
        val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }

        dataSource.transaction { session ->
            innTransaksjonMap[true]?.takeIf { it.isNotEmpty() }?.apply {
                val personIdList = this.map { it.personId!! }
                val forrigeTransaksjonMap = transaksjonRepository.findForrigeTransaksjonByPersonId(personIdList).associateBy { it.personId }
                val tidligereFagomradeMap = transaksjonRepository.findForrigeFagomradeByPersonId(personIdList)

                transaksjonRepository.insertBatch(this.map { it.toTransaksjon(forrigeTransaksjonMap[it.personId], tidligereFagomradeMap) }, session)

                val transaksjonIdList = this.map { innTransaksjon -> innTransaksjon.innTransaksjonId!! }
                transaksjonTilstandRepository.insertBatch(transaksjonIdList, session)

                logger.debug { "${transaksjonIdList.size} transaksjoner opprettet" }
            }

            innTransaksjonMap[false]?.takeIf { it.isNotEmpty() }?.apply {
                val avvikTransaksjonIdList = avvikTransaksjonRepository.insertBatch(this, session)
                logger.debug { "${avvikTransaksjonIdList.size} avvikstransaksjoner opprettet: " }
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
