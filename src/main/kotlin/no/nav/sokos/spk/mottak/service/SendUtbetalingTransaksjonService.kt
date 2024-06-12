package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdrag110
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdragsLinje150
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

class SendUtbetalingTransaksjonService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val producer: JmsProducerService = JmsProducerService(),
) {
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    fun hentUtbetalingTransaksjonOgSendTilOppdrag() {
        val timer = Instant.now()
        var totalTransaksjoner = 0

        runCatching {
            val transaksjonList = transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
            if (transaksjonList.isNotEmpty()) {
                logger.info { "Start å sende utbetaling melding til OppdragZ" }

                val transaksjonMap = transaksjonList.groupBy { Pair(it.personId, it.gyldigKombinasjon.fagomrade!!) }
                transaksjonMap.forEach { (key, transaksjon) ->
                    sendTilOppdrag(key, transaksjon)
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
        key: Pair<Int, String?>,
        transaksjonList: List<Transaksjon>,
    ) {
        val transaksjonIdList = transaksjonList.map { it.transaksjonId!! }
        dataSource.transaction { session ->
            runCatching {
                val transaksjon = transaksjonList.first()
                val oppdrag =
                    ObjectFactory().createOppdrag(
                        Oppdrag().apply {
                            oppdrag110 =
                                transaksjon.oppdrag110().apply {
                                    oppdragsLinje150.addAll(transaksjonList.map { it.oppdragsLinje150() })
                                }
                        },
                    )
                producer.send(JaxbUtils.marshall(oppdrag), PropertiesConfig.MQProperties().utbetalingQueueName)
                transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_OK, session)
                transaksjonTilstandRepository.insertBatch(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_OK, session)
            }.onFailure { exception ->
                transaksjonRepository.updateTransTilstandStatus(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_FEIL, session)
                transaksjonTilstandRepository.insertBatch(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_SENDT_FEIL, session)
                logger.error(exception) { "TransaksjonsId: ${transaksjonList.joinToString()} sendt til OppdragZ får ikke sendt til OppdragZ. " }
            }
        }
    }
}
