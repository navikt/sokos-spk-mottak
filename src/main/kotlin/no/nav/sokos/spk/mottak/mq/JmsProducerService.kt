package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jms.MQDestination
import com.ibm.mq.jms.MQQueue
import com.ibm.msg.client.wmq.common.CommonConstants
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.metrics.Metrics
import javax.jms.ConnectionFactory
import javax.jms.JMSContext
import javax.jms.JMSContext.AUTO_ACKNOWLEDGE

private val logger = KotlinLogging.logger {}

class JmsProducerService(
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext()

    fun send(
        payload: String,
        queueName: String,
        replyQueueName: String,
    ) {
        jmsContext.createContext(AUTO_ACKNOWLEDGE).use { context ->
            val destination = context.createQueue(MQQueue(queueName).queueURI)
            (destination as MQDestination).targetClient = CommonConstants.WMQ_TARGET_DEST_MQ
            (destination as MQDestination).messageBodyStyle = CommonConstants.WMQ_MESSAGE_BODY_MQ

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
