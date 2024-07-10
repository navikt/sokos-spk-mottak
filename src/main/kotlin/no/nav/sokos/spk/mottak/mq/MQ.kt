package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jakarta.jms.MQQueue
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig

private val logger = KotlinLogging.logger {}

class MQ(
    private val senderQueue: MQQueue,
    private val replyQueue: MQQueue,
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext(JMSContext.SESSION_TRANSACTED)

    fun send(message: String) {
        logger.debug("Sender melding til ${senderQueue.baseQueueName}")
        val producer = jmsContext.createProducer().apply { jmsReplyTo = replyQueue }
        jmsContext.use { ctx ->
            runCatching {
                producer.send(senderQueue, message)
            }.onSuccess {
                ctx.commit()
                logger.debug("MQ-transaksjon committed {}", ctx)
            }.onFailure {
                ctx.rollback()
                logger.error("MQ-transaksjon rolled back {}", ctx)
            }
        }
    }
}
