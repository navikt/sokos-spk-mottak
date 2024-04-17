package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import java.time.Instant
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.isTransaksjonStatusOK
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository

private val logger = KotlinLogging.logger {}

class BehandleUtbetalingerTrekkService(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource()
) {
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource)
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)

    fun behandlerInntransaksjon() {
        val timer = Instant.now()
        var count = 0

        runCatching {
            // henter ut alle fullmakter
            // validering alle rader i T_INN_TRANSAKSJON
            executeInntransaksjonValidation()

            while (true) {
                val innTransaksjonList = innTransaksjonRepository.getByTransaksjonStatusIsNullWithPersonId()
                count += innTransaksjonList.size

                when {
                    innTransaksjonList.isNotEmpty() -> behandlerInntransaksjon(innTransaksjonList)
                    else -> {
                        if (count > 0) {
                            logger.info { "Total $count InnTransaksjoner validert på ${Duration.between(timer, Instant.now()).toSeconds()} sekunder" }
                        }
                        break
                    }
                }
            }
        }.onFailure { exception ->
            logger.error(exception) { "Alvorlig feil under behandling av utbetalinger og trekk." }
        }
    }

    private fun behandlerInntransaksjon(innTransaksjonList: List<InnTransaksjon>) {
        dataSource.transaction {
            innTransaksjonList.forEach { innTransaksjon ->
                if (innTransaksjon.isTransaksjonStatusOK()) {

                } else {

                }
            }
        }
    }

    private fun executeInntransaksjonValidation() {
        dataSource.transaction { session ->
            innTransaksjonRepository.validateTransaksjon(session)
        }
    }
}