package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.jms.JmsConstants.SESSION_TRANSACTED
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.JMSProducer
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics

private val logger = KotlinLogging.logger {}

open class JmsProducerService(
    private val senderQueue: MQQueue,
    private val replyQueue: MQQueue,
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext()
    private val producer: JMSProducer = jmsContext.createProducer().apply { jmsReplyTo = replyQueue }

    open fun send(payload: List<String>) {
        jmsContext.createContext(SESSION_TRANSACTED).use { context ->
            val messages = payload.map { context.createTextMessage(it) }
            runCatching {
                messages.forEach { message ->
                    producer.send(senderQueue, message)
                    logger.debug { "sendt message $message" }
                }
            }.onSuccess {
                context.commit()
                Metrics.mqProducerMetricCounter.inc(payload.size.toLong())
                logger.debug { "MQ-transaksjon committed ${payload.size} messages" }
            }.onFailure { exception ->
                context.rollback()
                logger.error(exception) { "MQ-transaksjon rolled back" }
                throw MottakException(exception.message!!)
            }
        }
    }
}
