package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

class SendTransaksjonService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val producer: JmsProducerService = JmsProducerService(),
) {
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)

    fun sendUtbetalingTilOppdrag() {
        val timer = Instant.now()
        var totalTransaksjoner = 0

        runCatching {
            val transaksjonList = transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
            if (transaksjonList.isNotEmpty()) {
                logger.info { "Start å sende melding til OppdragZ" }

                val transaksjonMap = transaksjonList.groupBy { it.personId }
                transaksjonMap.forEach { (personId, transaksjon) ->
                    sendTilOppdrag(personId, transaksjon)

                    totalTransaksjoner += transaksjon.size
                }

                logger.info { "$totalTransaksjoner transaksjoner sendt til OppdragZ på ${Duration.between(timer, Instant.now()).toSeconds()} sekunder. " }
            }
        }.onFailure { exception ->
            val errorMessage = "Feil under sending returfil til SPK. Feilmelding: ${exception.message}"
            logger.error(exception) { errorMessage }
            throw MottakException(errorMessage)
        }
    }

    private fun sendTilOppdrag(
        personId: Int,
        transaksjonList: List<Transaksjon>,
    ) {
    }
}
