package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jms.MQQueue
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig
import java.util.UUID
import javax.jms.JMSContext
import javax.jms.JMSProducer

class MQ() {
    private val logger = KotlinLogging.logger {}

    private val context: JMSContext
        get() =
            MQConfig.connectionFactory().createContext(
                JMSContext.SESSION_TRANSACTED,
            )

    fun <T : Any> transaction(block: (JMSContext) -> T): T {
        return context.use { ctx ->
            logger.debug("MQ-transaksjon opprettet {}", ctx)
            val result =
                runCatching {
                    block(ctx)
                }.onSuccess {
                    ctx.commit()
                    logger.debug("MQ-transaksjon committed {}", ctx)
                }.onFailure {
                    ctx.rollback()
                    logger.error("MQ-transaksjon rolled back {}", ctx)
                }
            return@use result.getOrThrow()
        }
    }
}

class MQSender(
    private val mq: MQ,
    private val queue: MQQueue,
) {
    private val logger = KotlinLogging.logger {}

    fun send(
        message: String,
        config: JMSProducer.() -> Unit = {},
    ) {
        logger.debug("Sender melding til ${queue.baseQueueName}")
        mq.transaction { ctx ->
            ctx.clientID = UUID.randomUUID().toString()
            val producer = ctx.createProducer().apply(config)
            producer.send(queue, message)
        }
    }
}
