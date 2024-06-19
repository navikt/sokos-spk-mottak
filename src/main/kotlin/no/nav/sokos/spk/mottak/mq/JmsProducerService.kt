package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jakarta.jms.MQDestination
import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.JMSContext.AUTO_ACKNOWLEDGE
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.metrics.Metrics

private val logger = KotlinLogging.logger {}

open class JmsProducerService(
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext()

    open fun send(
        payload: String,
        queueName: String,
        replyQueueName: String,
    ) {
        jmsContext.createContext(AUTO_ACKNOWLEDGE).use { context ->
            val destination = context.createQueue(MQQueue(queueName).queueURI)
            (destination as MQDestination).targetClient = WMQConstants.WMQ_TARGET_DEST_MQ
            (destination as MQDestination).messageBodyStyle = WMQConstants.WMQ_MESSAGE_BODY_MQ

            val message =
                context.createTextMessage(payload).apply {
                    jmsReplyTo = MQQueue(replyQueueName)
                }
            context.createProducer().send(destination, message)
            logger.debug { "Sent melding til OppdragZ, messageId: ${message.jmsMessageID}, payload: $payload" }
        }
        Metrics.mqProducerMetricsCounter.inc()
    }
}
