package no.nav.sokos.spk.mottak.mq

import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.util.JaxbUtils

private val logger = KotlinLogging.logger {}

class JmsConsumerListenerService(
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)
    private val utbetalingReplyQueueName = PropertiesConfig.MQProperties().utbetalingReplyQueueName
    private val utbetalingConsumer = jmsContext.createConsumer(jmsContext.createQueue(utbetalingReplyQueueName))

    init {
        utbetalingConsumer.setMessageListener { message ->
            runCatching {
                val jmsMessage =
                    message.getBody(String::class.java).also {
                        logger.info { "Mottatt melding fra OppdragZ, mesageId: ${message.jmsMessageID}, message: $it" }
                    }
                val oppdrag = JaxbUtils.unmarshall(jmsMessage)
                // TODO: skal oppdatere DB med ORO (OPPDRAG RETUR OK)
                println(oppdrag)
                Metrics.mqConsumerMetricsCounter.inc()
            }.onFailure { exception ->
                logger.error(exception) {
                    "Feil ved prosessering av melding fra: $utbetalingReplyQueueName"
                }
            }
        }
        jmsContext.setExceptionListener { exception -> logger.error("Feil mot $utbetalingReplyQueueName", exception) }
        jmsContext.start()
    }
}
