package no.nav.sokos.spk.mottak.mq

import com.zaxxer.hikari.HikariDataSource
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.Message
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class JmsListenerService(
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)
    private val utbetalingReplyQueueName = PropertiesConfig.MQProperties().utbetalingReplyQueueName
    private val utbetalingMQListener = jmsContext.createConsumer(jmsContext.createQueue(utbetalingReplyQueueName))
    private val trekkReplyQueueName = PropertiesConfig.MQProperties().trekkReplyQueueName
    private val trekkMQListener = jmsContext.createConsumer(jmsContext.createQueue(utbetalingReplyQueueName))

    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    init {
        utbetalingMQListener.setMessageListener { message -> onUtbetalingMessage(message) }
        trekkMQListener.setMessageListener { message -> onTrekkMessage(message) }

        jmsContext.setExceptionListener { exception -> logger.error("Feil mot $utbetalingReplyQueueName", exception) }
        jmsContext.start()
    }

    private fun onUtbetalingMessage(message: Message) {
        runCatching {
            val jmsMessage =
                message.getBody(String::class.java).also { content ->
                    logger.info { "Mottatt melding fra OppdragZ, mesageId: ${message.jmsMessageID}" }
                    logger.debug { "mesageId: ${message.jmsMessageID}, message content: $content" }
                }
            val oppdrag = JaxbUtils.unmarshallOppdrag(jmsMessage)
            if (oppdrag.mmel.alvorlighetsgrad.toInt() < 5) {
                Metrics.mqUtbetalingListenerMetricCounter.inc()
            }
            // TODO: skal oppdatere DB med ORO (OPPDRAG RETUR OK)
            // println(oppdrag)

            exitProcess(1)
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved prosessering av melding fra: $utbetalingReplyQueueName" }
        }
    }

    private fun onTrekkMessage(message: Message) {
    }
}
