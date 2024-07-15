package no.nav.sokos.spk.mottak.mq

import com.ibm.msg.client.jakarta.jms.JmsConstants.SESSION_TRANSACTED
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.Queue
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics

private val logger = KotlinLogging.logger {}

open class JmsProducerService(
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext()

    open fun send(
        payload: List<String>,
        queue: Queue,
        replyQueue: Queue,
    ) {
        jmsContext.createContext(SESSION_TRANSACTED).use { context ->
            runCatching {
                val producer = context.createProducer().apply { jmsReplyTo = replyQueue }
                payload.forEach { message ->
                    val jmsMessage = context.createTextMessage(message)
                    producer.send(queue, jmsMessage)
                    logger.debug { "Sent melding til OppdragZ, messageId: ${jmsMessage.jmsMessageID}, payload: $payload" }
                }
            }.onSuccess {
                context.commit()
                Metrics.mqProducerMetricCounter.inc(payload.size.toLong())
                logger.debug { "MQ-transaksjon committed ${payload.size} message" }
            }.onFailure { exception ->
                context.rollback()
                logger.error(exception) { "MQ-transaksjon rolled back" }
                throw MottakException(exception.message!!)
            }
        }
    }
}
