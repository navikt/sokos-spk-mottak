package no.nav.sokos.spk.mottak.mq

import kotlinx.serialization.json.Json

import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSConsumer
import jakarta.jms.JMSContext
import jakarta.jms.MessageListener
import jakarta.jms.Queue
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

interface ManagedMqListener : AutoCloseable {
    fun start()
}

abstract class JmsListener(
    connectionFactory: ConnectionFactory,
) : ManagedMqListener {
    protected val jmsContext: JMSContext = connectionFactory.createContext(JMSContext.CLIENT_ACKNOWLEDGE)
    protected val json = Json { ignoreUnknownKeys = true }

    init {
        try {
            jmsContext.setExceptionListener { logger.error("Feil på MQ-kommunikasjon", it) }
        } catch (exception: Exception) {
            closeAfterConstructionFailure(exception)
            throw exception
        }
    }

    protected fun createConsumer(
        queue: Queue,
        messageListener: MessageListener,
    ): JMSConsumer =
        try {
            jmsContext.createConsumer(queue).apply { setMessageListener(messageListener) }
        } catch (exception: Exception) {
            closeAfterConstructionFailure(exception)
            throw exception
        }

    override fun start() {
        jmsContext.start()
    }

    override fun close() {
        jmsContext.close()
    }

    private fun closeAfterConstructionFailure(exception: Exception) {
        try {
            close()
        } catch (closeException: Exception) {
            exception.addSuppressed(closeException)
        }
    }
}
