package no.nav.sokos.spk.mottak.mq

import kotlinx.serialization.json.Json

import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.Session
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

abstract class JmsListener(
    connectionFactory: ConnectionFactory,
) {
    private val connection = connectionFactory.createConnection()
    protected val session: Session = connection.createSession(true, JMSContext.CLIENT_ACKNOWLEDGE)
    protected val json = Json { ignoreUnknownKeys = true }

    init {
        connection.setExceptionListener { logger.error("Feil på MQ-kommunikasjon", it) }
    }

    fun start() {
        connection.start()
    }
}
