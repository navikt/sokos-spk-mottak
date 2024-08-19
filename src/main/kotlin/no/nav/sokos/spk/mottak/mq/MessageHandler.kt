package no.nav.sokos.spk.mottak.mq

import com.zaxxer.hikari.HikariDataSource
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils

abstract class MessageHandler(
    val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    abstract fun handle(jmsMessage: String)
}

class UtbetalingMessageHandler : MessageHandler() {
    override fun handle(jmsMessage: String) {
        val oppdrag = JaxbUtils.unmarshallOppdrag(jmsMessage)
    }
}

class TrekkMessageHandler : MessageHandler() {
    private val logger = KotlinLogging.logger { }

    override fun handle(jmsMessage: String) {
        runCatching {
            val trekk = JaxbUtils.unmarshallTrekk(jmsMessage)
            val trekkStatus = trekk.mmel?.kodeMelding.let { "TRF" } ?: "TRO"
            using(sessionOf(dataSource)) { session ->
                val transaksjonId = transaksjonRepository.getByTransEksIdFk(trekk.innrapporteringTrekk?.kreditorTrekkId!!, session)
                val transtilstandId =
                    transaksjonTilstandRepository.insertTransaksjonTilstand(
                        transaksjonId!!,
                        trekkStatus,
                        session,
                    )!!
                transaksjonRepository.updateTransaksjonFromTrekkReply(
                    transaksjonId,
                    transtilstandId,
                    trekk.innrapporteringTrekk?.navTrekkId!!,
                    trekk.mmel?.kodeMelding.let { "TRF" } ?: "TRO",
                    trekk.mmel?.kodeMelding ?: "",
                    trekk.mmel?.beskrMelding ?: "",
                    session,
                )
            }
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved deserialisering av melding: $jmsMessage" }
        }
    }
}
