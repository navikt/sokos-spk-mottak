package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.AndreTrekk
import no.nav.sokos.spk.mottak.domain.EndringsInfo
import no.nav.sokos.spk.mottak.domain.Fagomrade
import no.nav.sokos.spk.mottak.domain.SPK_TSS
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.domain.Transaksjon
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

        val transaksjoner = transaksjonRepository.findAllByTrekkBelopstypeAndByTransaksjonTilstand(TRANS_TILSTAND_TIL_TREKK)
        if (transaksjoner.isNotEmpty()) {
            logger.info { "Starter sending av trekktransaksjoner til OppdragZ" }
            transaksjoner.chunked(BATCH_SIZE).forEach {
                val transaksjonIdList = it.map { it.transaksjonId!! }
                val trekkMeldinger: MutableList<String> = mutableListOf()
                it.forEach { transaksjon ->
                    trekkMeldinger.add(JaxbUtils.marshall(opprettAndreTrekk(transaksjon)))
                }
                trekkSender.send(trekkMeldinger.joinToString(separator = "")) {
                    jmsReplyTo = replyQueueTrekk
                }
                totalTransaksjoner += transaksjonIdList.size
                dataSource.transaction { session ->
                    runCatching {
                        transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_OK, session)
                        transaksjonTilstandRepository.insertBatch(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_OK, session)
                        logger.info { "$totalTransaksjoner trekktransaksjoner sendt til OppdragZ brukte ${Duration.between(timer, Instant.now()).toSeconds()} sekunder. " }
                    }.onFailure { exception ->
                        transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_FEIL, session)
                        transaksjonTilstandRepository.insertBatch(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_FEIL, session)
                        logger.error(exception) { "TransaksjonsId: ${transaksjonIdList.min()} - ${transaksjonIdList.max()} feiler ved sending til OppdragZ. " }
                    }
                }
            }
        }
    }

    fun opprettAndreTrekk(transaksjon: Transaksjon) =
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
