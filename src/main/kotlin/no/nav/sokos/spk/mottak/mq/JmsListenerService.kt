package no.nav.sokos.spk.mottak.mq

import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.metrics.Metrics

private val logger = KotlinLogging.logger {}

class JmsListenerService(
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
    private val replyQueueName: String,
    private val messageHandler: MessageHandler,
) {
    private val jmsContext: JMSContext = connectionFactory.createContext()
    private val messageConsumer = jmsContext.createConsumer(jmsContext.createQueue(replyQueueName))

    init {
        messageConsumer.setMessageListener { message ->
            runCatching {
                val jmsMessage = message.getBody(String::class.java)
                logger.info { "Mottatt melding fra OppdragZ, mesageId: ${message.jmsMessageID}, message: $jmsMessage" }
                messageHandler.handle(jmsMessage)
                Metrics.mqConsumerMetricCounter.inc()
            }.onFailure { exception ->
                logger.error(exception) { "Feil ved prosessering av melding fra: $replyQueueName" }
            }
        }
        jmsContext.setExceptionListener { exception -> logger.error("Feil mot $replyQueueName", exception) }
        jmsContext.start()
    }
}
