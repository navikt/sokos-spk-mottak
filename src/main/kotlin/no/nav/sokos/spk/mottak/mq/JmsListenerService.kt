package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jakarta.jms.MQQueue
import com.zaxxer.hikari.HikariDataSource
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.Message
import jakarta.jms.Queue
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_RETUR_OK
import no.nav.sokos.spk.mottak.metrics.Metrics.mqTrekkListenerMetricCounter
import no.nav.sokos.spk.mottak.metrics.Metrics.mqUtbetalingListenerMetricCounter
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils

private val logger = KotlinLogging.logger {}

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

            val transaksjonIdList = oppdrag.oppdrag110.oppdragsLinje150.map { it.delytelseId.toInt() }

            dataSource.transaction { session ->
                transaksjonRepository.updateTransTilstandStatus(
                    transaksjonIdList,
                    transTilstandStatus,
                    null,
                    session,
                )
                transaksjonTilstandRepository.insertBatch(
                    transaksjonIdList,
                    transTilstandStatus,
                    oppdrag.mmel.kodeMelding,
                    oppdrag.mmel.beskrMelding,
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
            logger.debug { "Mottatt trekk-melding fra OppdragZ, message content: $jmsMessage" }
            val trekk = JaxbUtils.unmarshallTrekk(jmsMessage)
            val trekkStatus = trekk.mmel?.kodeMelding.let { TRANS_TILSTAND_TREKK_RETUR_FEIL } ?: TRANS_TILSTAND_TREKK_RETUR_OK

            dataSource.transaction { session ->
                val transaksjonId = transaksjonRepository.getByTransEksIdFk(trekk.innrapporteringTrekk?.kreditorTrekkId!!)!!

                transaksjonTilstandRepository.insertBatch(
                    listOf(transaksjonId),
                    trekkStatus,
                    trekk.mmel?.kodeMelding.orEmpty(),
                    trekk.mmel?.beskrMelding.orEmpty(),
                    session,
                )

                transaksjonRepository.updateTransTilstandStatus(
                    listOf(transaksjonId),
                    trekkStatus,
                    trekk.innrapporteringTrekk.navTrekkId,
                    session,
                )
            }
            mqTrekkListenerMetricCounter.inc()
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved prosessering av trekksmelding : ${message.jmsMessageID}" }
        }
    }
}
