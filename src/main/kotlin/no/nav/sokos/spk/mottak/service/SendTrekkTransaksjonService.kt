package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
import com.zaxxer.hikari.HikariDataSource
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.AndreTrekk
import no.nav.sokos.spk.mottak.domain.EndringsInfo
import no.nav.sokos.spk.mottak.domain.Fagomrade
import no.nav.sokos.spk.mottak.domain.SPK_TSS
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.mq.MQ
import no.nav.sokos.spk.mottak.mq.MQSender
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }
private const val BATCH_SIZE = 1000

class SendTrekkTransaksjonService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)
    private val trekkSender = MQSender(MQ(), MQQueue(PropertiesConfig.MQProperties().trekkSenderQueueName))
    private val replyQueueTrekk = MQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName)

    fun sendTrekkTilOppdrag() {
        val timer = Instant.now()
        var totalTransaksjoner = 0

        val transaksjoner =
            runCatching {
                transaksjonRepository.findAllByTrekkBelopstypeAndByTransaksjonTilstand(TRANS_TILSTAND_TIL_TREKK)
            }.getOrElse { exception ->
                val errorMessage = "Feil under henting av trekktransaksjoner. Feilmelding: ${exception.message}"
                logger.error(exception) { errorMessage }
                throw MottakException(errorMessage)
            }
        if (transaksjoner.isEmpty()) return

        logger.info { "Starter sending av trekktransaksjoner til OppdragZ" }
        transaksjoner.chunked(BATCH_SIZE).forEach { chunk ->
            val transaksjonIdList = chunk.mapNotNull { it.transaksjonId }
            val transaksjonTilstandIdList = updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_OK)
            val trekkMeldinger = chunk.map { JaxbUtils.marshallTrekk(opprettAndreTrekk(it)) }
            runCatching {
                trekkSender.send(trekkMeldinger.joinToString(separator = "")) {
                    jmsReplyTo = replyQueueTrekk
                }
                totalTransaksjoner += transaksjonIdList.size
                logger.info { "$totalTransaksjoner trekktransaksjoner sendt til OppdragZ brukte ${Duration.between(timer, Instant.now()).toSeconds()} sekunder. " }
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
    ): List<Int> {
        return using(sessionOf(dataSource)) { session ->
            transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, transTilstandStatus, session)
            transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session)
        }
    }

    private fun opprettAndreTrekk(transaksjon: Transaksjon) =
        AndreTrekk(
            debitorOffnr = transaksjon.fnr,
            trekktypeKode = transaksjon.trekkType!!,
            trekkperiodeFom = transaksjon.datoFom!!,
            trekkperiodeTom = transaksjon.datoTom!!,
            kreditorRef = transaksjon.transEksId,
            tssEksternId = SPK_TSS,
            trekkAlternativKode = transaksjon.trekkAlternativ!!,
            sats = transaksjon.belop / 100.0,
            endringsInfo = EndringsInfo(opprettetAvId = transaksjon.opprettetAv, kildeId = SPK_TSS),
            fagomradeListe = listOf(Fagomrade(trekkgruppeKode = transaksjon.trekkGruppe!!)),
        )
}
