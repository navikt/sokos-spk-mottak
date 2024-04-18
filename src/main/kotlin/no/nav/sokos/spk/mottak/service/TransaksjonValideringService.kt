package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import java.time.Instant
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.toTransaksjon
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.integration.FullmaktClientService
import no.nav.sokos.spk.mottak.repository.AvvikTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository

private val logger = KotlinLogging.logger {}

class TransaksjonValideringService(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource(),
    private val fullmaktClientService: FullmaktClientService = FullmaktClientService()
) {
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource)
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)
    private val avvikTransaksjonRepository: AvvikTransaksjonRepository = AvvikTransaksjonRepository(dataSource)

    fun validereTransaksjon() {
        val timer = Instant.now()
        var count = 0

        runCatching {
            // henter ut alle fullmakter
            val fullmaktMap = fullmaktClientService.getFullmakt().ifEmpty { throw MottakException("Mangler fullmakter") }

            // validering alle rader i T_INN_TRANSAKSJON
            executeInntransaksjonValidation()

            while (true) {
                val innTransaksjonList = innTransaksjonRepository.getByTransaksjonStatusIsNullWithPersonId()
                count += innTransaksjonList.size
                logger.info { "Antall InnTransaksjon blir hentet: ${innTransaksjonList.size}" }

                when {
                    innTransaksjonList.isNotEmpty() -> validereTransaksjon(innTransaksjonList)
                    else -> {
                        if (count > 0) {
                            logger.info { "Total $count InnTransaksjoner validert på ${Duration.between(timer, Instant.now()).toSeconds()} sekunder" }
                        } else {
                            logger.info { "Finner ingen Inntransaksjoner ubehandlet." }
                        }
                        break
                    }
                }
            }
        }.onFailure { exception ->
            logger.error(exception) { "Alvorlig feil under behandling av utbetalinger og trekk." }
        }
    }

    private fun validereTransaksjon(innTransaksjonList: List<InnTransaksjon>) {
        val innTransaksjonMap = innTransaksjonList.groupBy { it.transaksjonStatus == TRANSAKSJONSTATUS_OK }

        dataSource.transaction { session ->
            innTransaksjonMap[true]?.takeIf { it.isNotEmpty() }?.apply {
                val transaksonMap = transaksjonRepository.getLastTransaksjonByPersonId(this.map { it.personId!! }).associateBy { it.personId }
                transaksjonRepository.insertBatch(this.map { it.toTransaksjon(transaksonMap[it.personId]) }, session)

                val transakjonIdList = this.map { it.innTransaksjonId!! }
                transaksjonTilstandRepository.insertBatch(transakjonIdList, session)

                logger.info { "Antall transaksjoner blir opprettet: ${transakjonIdList.size}" }
            }

            innTransaksjonMap[false]?.takeIf { it.isNotEmpty() }?.apply {
                val avvikTransaksjonIdList = avvikTransaksjonRepository.insertBatch(this, session)
                logger.info { "Antall avvik transaksjoner blir opprettet: ${avvikTransaksjonIdList.size}" }
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