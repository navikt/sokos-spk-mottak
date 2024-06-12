package no.nav.sokos.spk.mottak.mq

import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.util.JaxbUtils
import javax.jms.ConnectionFactory
import javax.jms.JMSContext

private val logger = KotlinLogging.logger {}

class JmsConsumerListenerService(
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
) {
    private val mqQueueManagerName = PropertiesConfig.MQProperties().mqQueueManagerName
    private val jmsContext: JMSContext = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)
    private val utbetalingConsumer = jmsContext.createConsumer(jmsContext.createQueue(mqQueueManagerName))

    init {
        utbetalingConsumer.setMessageListener { message ->
            runCatching {
                message.getBody(String::class.java).let {
                    logger.debug { "Mottatt melding fra OppdragZ, mesageId: ${message.jmsMessageID}, message: $it" }
                }
                JaxbUtils.unmarshall(message.getBody(String::class.java))
                Metrics.mqConsumerMetricsCounter.inc()
            }.onFailure { exception ->
                logger.error(exception) {
                    "Feil ved prosessering av melding fra: $mqQueueManagerName"
                }
            }
        }
        jmsContext.setExceptionListener { exception -> logger.error("Feil mot $mqQueueManagerName", exception) }
        jmsContext.start()
    }
}
