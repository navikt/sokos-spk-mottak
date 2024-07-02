package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
import com.zaxxer.hikari.HikariDataSource
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.Aksjonskode
import no.nav.sokos.spk.mottak.domain.Content
import no.nav.sokos.spk.mottak.domain.DebitorId
import no.nav.sokos.spk.mottak.domain.Document
import no.nav.sokos.spk.mottak.domain.Identifisering
import no.nav.sokos.spk.mottak.domain.KodeTrekkAlternativ
import no.nav.sokos.spk.mottak.domain.KodeTrekktype
import no.nav.sokos.spk.mottak.domain.Kreditor
import no.nav.sokos.spk.mottak.domain.MsgInfo
import no.nav.sokos.spk.mottak.domain.MsgType
import no.nav.sokos.spk.mottak.domain.Organisation
import no.nav.sokos.spk.mottak.domain.Periode
import no.nav.sokos.spk.mottak.domain.Receiver
import no.nav.sokos.spk.mottak.domain.RefDoc
import no.nav.sokos.spk.mottak.domain.SPK_TSS
import no.nav.sokos.spk.mottak.domain.Sats
import no.nav.sokos.spk.mottak.domain.Sender
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.Trekk
import no.nav.sokos.spk.mottak.domain.TrekkInfo
import no.nav.sokos.spk.mottak.domain.TrekkMelding
import no.nav.sokos.spk.mottak.domain.Type
import no.nav.sokos.spk.mottak.domain.TypeId
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.mq.MQ
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import java.time.Duration
import java.time.Instant
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger { }
private const val BATCH_SIZE = 100
private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

class SendTrekkTransaksjonTilOppdragService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val trekkSender: MQ = MQ(MQQueue(PropertiesConfig.MQProperties().trekkSenderQueueName), MQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName)),
) {
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    fun hentTrekkTransaksjonOgSendTilOppdrag() {
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
            val transaksjonTilstandIdList = updateTransaksjonOgTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_OK)
            val trekkMeldinger = chunk.map { JaxbUtils.marshallTrekk(opprettTrekkMelding(it)) }
            runCatching {
                trekkSender.send(trekkMeldinger.joinToString(separator = ""))
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

    private fun opprettTrekkMelding(it: Transaksjon) =
        TrekkMelding(
            msgInfo =
                MsgInfo(
                    type = Type(),
                    sender =
                        Sender(
                            organisation = Organisation(),
                        ),
                    receiver =
                        Receiver(
                            organisation = Organisation(),
                        ),
                ),
            document =
                Document(
                    refDoc =
                        RefDoc(
                            msgType = MsgType(),
                            content =
                                Content(
                                    innrapporteringTrekk = opprettTrekk(it),
                                ),
                        ),
                ),
        )

    private fun opprettTrekk(transaksjon: Transaksjon) =
        TrekkInfo(
            aksjonskode = Aksjonskode(),
            identifisering =
                Identifisering(
                    kreditorTrekkId = transaksjon.transEksId,
                    debitorId =
                        DebitorId(
                            id = transaksjon.fnr,
                            typeId = TypeId(),
                        ),
                ),
            trekk =
                Trekk(
                    kodeTrekktype = KodeTrekktype(v = transaksjon.trekkType!!),
                    kodeTrekkAlternativ = KodeTrekkAlternativ(v = transaksjon.trekkAlternativ!!),
                    sats = Sats((transaksjon.belop / 100.0).toString()),
                ),
            periode =
                Periode(
                    periodeFomDato = transaksjon.datoFom!!.format(formatter),
                    periodeTomDato = transaksjon.datoTom!!.format(formatter),
                ),
            kreditor =
                Kreditor(
                    tssId = SPK_TSS,
                ),
        )
}
