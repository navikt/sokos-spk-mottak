package no.nav.sokos.spk.mottak.mq

import com.zaxxer.hikari.HikariDataSource
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.Message
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import kotlin.system.exitProcess

private val logger = KotlinLogging.logger {}

class JmsListenerService(
    private val connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    private val jmsContext: JMSContext = connectionFactory.createContext(JMSContext.AUTO_ACKNOWLEDGE)
    private val mqProperties = PropertiesConfig.MQProperties()
    private val utbetalingMQListener = createMQListener(mqProperties.utbetalingReplyQueueName)
    private val trekkMQListener = createMQListener(mqProperties.trekkReplyQueueName)
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    init {
        utbetalingMQListener.setMessageListener { onUtbetalingMessage(it) }
        trekkMQListener.setMessageListener { onTrekkMessage(it) }

        jmsContext.setExceptionListener { logger.error("Feil på MQ-kommunikasjon", it) }
        jmsContext.start()
    }

    private fun createMQListener(queueName: String) = jmsContext.createConsumer(jmsContext.createQueue(queueName))

    private fun onUtbetalingMessage(message: Message) {
        runCatching {
            val jmsMessage = message.getBody(String::class.java)
            logger.debug { "Mottatt oppdragsmelding fra OppdragZ, message content: $jmsMessage" }
            val oppdrag = JaxbUtils.unmarshallOppdrag(jmsMessage)
            oppdrag.mmel.alvorlighetsgrad.toInt().takeIf { it < 5 }?.let { Metrics.mqUtbetalingListenerMetricCounter.inc() }
            // TODO: skal oppdatere DB med ORO (OPPDRAG RETUR OK)
            // println(oppdrag)

            exitProcess(1)
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved prosessering av oppdragsmelding : ${message.jmsMessageID}" }
        }
    }

    private fun onTrekkMessage(message: Message) {
        runCatching {
            val jmsMessage = message.getBody(String::class.java)
            logger.debug { "Mottatt trekk-melding fra OppdragZ, message content: $jmsMessage" }
            val trekk = JaxbUtils.unmarshallTrekk(jmsMessage)
            val trekkStatus = trekk.mmel?.kodeMelding.let { "TRF" } ?: "TRO"

            using(sessionOf(dataSource)) { session ->
                val transaksjonId =
                    transaksjonRepository.getByTransEksIdFk(
                        trekk.innrapporteringTrekk?.kreditorTrekkId!!,
                        session,
                    )
                val transtilstandId =
                    transaksjonTilstandRepository.insertTransaksjonTilstand(
                        transaksjonId!!,
                        trekkStatus,
                        trekk.mmel?.kodeMelding.orEmpty(),
                        trekk.mmel?.beskrMelding.orEmpty(),
                        session,
                    )!!
                transaksjonRepository.updateTransaksjonFromTrekkReply(
                    transaksjonId,
                    transtilstandId,
                    trekk.innrapporteringTrekk?.navTrekkId!!,
                    trekkStatus,
                    session,
                )
            }
        }.onFailure { exception ->
            logger.error(exception) { "Feil ved prosessering av trekksmelding : ${message.jmsMessageID}" }
        }
    }
}
