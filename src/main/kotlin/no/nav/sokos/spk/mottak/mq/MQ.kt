package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jakarta.jms.MQQueue
import jakarta.jms.JMSContext
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig
import java.util.UUID

open class MQ(
    private val senderQueue: MQQueue,
    private val replyQueue: MQQueue,
    val context: JMSContext = MQConfig.connectionFactory().createContext(JMSContext.SESSION_TRANSACTED),
) {
    private val logger = KotlinLogging.logger {}

    open fun send(message: String) {
        logger.debug("Sender melding til ${senderQueue.baseQueueName}")
        context.clientID = UUID.randomUUID().toString()
        val producer = context.createProducer().apply { jmsReplyTo = replyQueue }
        context.use { ctx ->
            logger.debug("MQ-transaksjon opprettet {}", ctx)
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
