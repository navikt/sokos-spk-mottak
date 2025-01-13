package no.nav.sokos.spk.mottak.mq

import com.ibm.msg.client.jakarta.jms.JmsConstants.SESSION_TRANSACTED
import io.prometheus.metrics.core.metrics.Counter
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.JMSProducer
import jakarta.jms.Queue
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.exception.MottakException

private val logger = KotlinLogging.logger {}

open class JmsProducerService(
    private val senderQueue: Queue,
    private val replyQueue: Queue? = null,
    private val metricCounter: Counter,
    connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext()
    private val producer: JMSProducer = jmsContext.createProducer().apply { jmsReplyTo = replyQueue }

    open fun send(payload: List<String>) {
        jmsContext.createContext(SESSION_TRANSACTED).use { context ->
            val messages = payload.map { context.createTextMessage(it) }
            runCatching {
                messages.forEach { message ->
                    producer.send(senderQueue, message)
                }
            }.onSuccess {
                context.commit()
                metricCounter.inc(payload.size.toLong())
                logger.debug { "MQ-transaksjon committed ${messages.size} meldinger" }
            }.onFailure { exception ->
                context.rollback()
                logger.error(exception) { "MQ-transaksjon rolled back" }
                throw MottakException(exception.message ?: "Feil ved sending av melding til MQ")
            }
        }
    }
}
