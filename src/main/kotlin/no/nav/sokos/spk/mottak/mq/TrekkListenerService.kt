package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jakarta.jms.MQQueue
import com.zaxxer.hikari.HikariDataSource
import jakarta.jms.ConnectionFactory
import jakarta.jms.Message
import jakarta.jms.Queue
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SECURE_LOGGER
import no.nav.sokos.spk.mottak.domain.OS_STATUS_OK
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.TREKK_LISTENER_SERVICE
import no.nav.sokos.spk.mottak.domain.converter.TrekkConverter.trekkTilstandStatus
import no.nav.sokos.spk.mottak.domain.oppdrag.Dokument
import no.nav.sokos.spk.mottak.domain.oppdrag.DokumentWrapper
import no.nav.sokos.spk.mottak.domain.oppdrag.Mmel
import no.nav.sokos.spk.mottak.metrics.Metrics.mqTrekkListenerMetricCounter
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER)

class TrekkListenerService(
    connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
    trekkReplyQueue: Queue = MQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) : JmsListener(connectionFactory) {
    private val trekkMQListener = jmsContext.createConsumer(trekkReplyQueue)
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    init {
        trekkMQListener.setMessageListener { onTrekkMessage(it) }
    }

    private fun onTrekkMessage(message: Message) {
        val jmsMessage = message.getBody(String::class.java)
        runCatching {
            logger.debug { "Mottatt trekkmeldingretur fra OppdragZ. Meldingsinnhold: $jmsMessage" }
            val trekkWrapper = json.decodeFromString<DokumentWrapper>(jmsMessage)
            processTrekkMessage(trekkWrapper.dokument!!, trekkWrapper.mmel!!)
            message.acknowledge()
        }.onFailure { exception ->
            secureLogger.error { "Trekkmelding fra OppdragZ: $jmsMessage" }
            logger.error(exception) { "Prosessering av trekkmeldingretur feilet. ${message.jmsMessageID}" }
        }
    }

    private fun processTrekkMessage(
        trekk: Dokument,
        trekkInfo: Mmel,
    ) {
        val trekkStatus = trekkInfo.trekkTilstandStatus()
        val transaksjonId = trekk.transaksjonsId.toInt()
        if (!isDuplicate(transaksjonId, trekkInfo.alvorlighetsgrad)) {
            dataSource.transaction { session ->
                val transTilstandIdList =
                    transaksjonTilstandRepository.insertBatch(
                        transaksjonIdList = listOf(transaksjonId),
                        transaksjonTilstandType = trekkStatus,
                        systemId = TREKK_LISTENER_SERVICE,
                        feilkode = trekkInfo.kodeMelding,
                        feilkodeMelding = trekkInfo.beskrMelding,
                        session = session,
                    )

                transaksjonRepository.updateBatch(
                    transaksjonIdList = listOf(transaksjonId),
                    transTilstandIdList = transTilstandIdList,
                    transaksjonTilstandType = trekkStatus,
                    systemId = TREKK_LISTENER_SERVICE,
                    vedtaksId = trekk.innrapporteringTrekk.navTrekkId,
                    osStatus = trekkInfo.alvorlighetsgrad,
                    session = session,
                )
            }
            mqTrekkListenerMetricCounter.inc()
        }
    }

    private fun isDuplicate(
        transaksjonId: Int,
        osStatus: String,
    ): Boolean {
        return when {
            osStatus == OS_STATUS_OK -> false
            transaksjonRepository.getByTransaksjonId(transaksjonId)!!.osStatus == TRANSAKSJONSTATUS_OK -> {
                logger.info { "Transaksjon: $transaksjonId er allerede mottatt med OK-status" }
                true
            }

            else -> false
        }
    }
}
