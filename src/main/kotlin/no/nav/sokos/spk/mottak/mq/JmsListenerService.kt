package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jakarta.jms.MQQueue
import com.zaxxer.hikari.HikariDataSource
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.Message
import jakarta.jms.Queue
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_OK
import no.nav.sokos.spk.mottak.domain.converter.TrekkConverter.trekkTilstandStatus
import no.nav.sokos.spk.mottak.domain.oppdrag.Dokument
import no.nav.sokos.spk.mottak.domain.oppdrag.DokumentWrapper
import no.nav.sokos.spk.mottak.domain.oppdrag.Mmel
import no.nav.sokos.spk.mottak.metrics.Metrics.mqTrekkListenerMetricCounter
import no.nav.sokos.spk.mottak.metrics.Metrics.mqUtbetalingListenerMetricCounter
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils

private val logger = KotlinLogging.logger {}
private const val OS_STATUS_OK = "00"

class JmsListenerService(
    connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
    utbetalingReplyQueue: Queue = MQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
    trekkReplyQueue: Queue = MQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)
    private val utbetalingMQListener = jmsContext.createConsumer(utbetalingReplyQueue)
    private val trekkMQListener = jmsContext.createConsumer(trekkReplyQueue)

    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    private val json = Json { ignoreUnknownKeys = true }

    init {
        utbetalingMQListener.setMessageListener { onUtbetalingMessage(it) }
        trekkMQListener.setMessageListener { onTrekkMessage(it) }

        jmsContext.setExceptionListener { logger.error("Feil på MQ-kommunikasjon", it) }
    }

    fun start() {
        jmsContext.start()
    }

    private fun onUtbetalingMessage(message: Message) {
        runCatching {
            val jmsMessage = message.getBody(String::class.java)
            logger.debug { "Mottatt oppdragsmelding fra OppdragZ, message content: $jmsMessage" }
            val oppdrag = JaxbUtils.unmarshallOppdrag(jmsMessage)

            val transTilstandStatus =
                when {
                    oppdrag.mmel.alvorlighetsgrad.toInt() < 5 -> TRANS_TILSTAND_OPPDRAG_RETUR_OK
                    else -> TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
                }

            val transaksjonIdList =
                oppdrag.oppdrag110.oppdragsLinje150
                    .filter { !isDuplicate(it.delytelseId.toInt(), oppdrag.mmel.alvorlighetsgrad) }
                    .map { it.delytelseId.toInt() }

            if (transaksjonIdList.isEmpty()) {
                logger.warn { "Ingen nye transaksjoner å prosessere!" }
                return
            }

            dataSource.transaction { session ->
                val transTilstandIdList =
                    transaksjonTilstandRepository.insertBatch(
                        transaksjonIdList,
                        transTilstandStatus,
                        oppdrag.mmel.kodeMelding,
                        oppdrag.mmel.beskrMelding,
                        session,
                    )

                transaksjonRepository.updateBatch(
                    transaksjonIdList,
                    transTilstandIdList,
                    transTilstandStatus,
                    null,
                    oppdrag.mmel.alvorlighetsgrad,
                    session,
                )
            }
            mqUtbetalingListenerMetricCounter.inc(transaksjonIdList.size.toLong())
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved prosessering av oppdragsmelding : ${message.jmsMessageID}" }
        }
    }

    private fun onTrekkMessage(message: Message) {
        runCatching {
            val jmsMessage = message.getBody(String::class.java)
            logger.debug { "Mottatt trekkmelding fra OppdragZ, message content: $jmsMessage" }
            val trekkWrapper = json.decodeFromString<DokumentWrapper>(jmsMessage)
            processTrekkMessage(trekkWrapper.dokument!!, trekkWrapper.mmel!!)
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved prosessering av trekkmelding : ${message.jmsMessageID}" }
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
                        listOf(transaksjonId),
                        trekkStatus,
                        trekkInfo.kodeMelding,
                        trekkInfo.beskrMelding,
                        session,
                    )

                transaksjonRepository.updateBatch(
                    listOf(transaksjonId),
                    transTilstandIdList,
                    trekkStatus,
                    trekk.innrapporteringTrekk?.navTrekkId ?: null,
                    trekkInfo.alvorlighetsgrad,
                    session,
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
                logger.warn { "Transaksjon: $transaksjonId er allerede mottatt med OK-status" }
                return true
            }

            else -> false
        }
    }
}
