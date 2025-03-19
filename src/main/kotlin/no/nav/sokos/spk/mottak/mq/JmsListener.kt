package no.nav.sokos.spk.mottak.mq

import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class JmsListener(
    connectionFactory: ConnectionFactory,
) {
    protected val jmsContext: JMSContext = connectionFactory.createContext(JMSContext.CLIENT_ACKNOWLEDGE)

    init {
        jmsContext.setExceptionListener { logger.error("Feil på MQ-kommunikasjon", it) }
    }

    fun start() {
        jmsContext.start()
    }
}
