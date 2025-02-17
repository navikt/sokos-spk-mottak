package no.nav.sokos.spk.mottak.mq

import kotlinx.serialization.json.Json

import com.ibm.mq.jakarta.jms.MQQueue
import com.zaxxer.hikari.HikariDataSource
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSContext
import jakarta.jms.Message
import jakarta.jms.Queue
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SECURE_LOGGER
import no.nav.sokos.spk.mottak.domain.avregning.Avregningsgrunnlag
import no.nav.sokos.spk.mottak.domain.avregning.AvregningsgrunnlagWrapper
import no.nav.sokos.spk.mottak.domain.avregning.Avregningsretur
import no.nav.sokos.spk.mottak.domain.converter.AvregningConverter.toAvregningsretur
import no.nav.sokos.spk.mottak.dto.Avregningstransaksjon
import no.nav.sokos.spk.mottak.repository.AvregningsreturRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction
import no.nav.sokos.spk.mottak.util.Utils.stringToInt
import no.nav.sokos.spk.mottak.util.Utils.toIsoDate
import no.nav.sokos.spk.mottak.util.Utils.toLocalDate

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER)

class AvregningService(
    connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
    avregningsgrunnlagQueue: Queue = MQQueue(PropertiesConfig.MQProperties().avregningsgrunnlagQueueName),
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) {
    private val jmsContext: JMSContext = connectionFactory.createContext(JMSContext.CLIENT_ACKNOWLEDGE)
    private val avregningsgrunnlagMQListener = jmsContext.createConsumer(avregningsgrunnlagQueue)
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val avregningsreturRepository: AvregningsreturRepository = AvregningsreturRepository(dataSource)

    private val json = Json { ignoreUnknownKeys = true }

    init {
        avregningsgrunnlagMQListener.setMessageListener { onAvregningsgrunnlagMessage(it) }
        jmsContext.setExceptionListener { logger.error("Feil på MQ-kommunikasjon med UR", it) }
    }

    fun start() {
        jmsContext.start()
    }

    private fun onAvregningsgrunnlagMessage(message: Message) {
        val jmsMessage = message.getBody(String::class.java)
        runCatching {
            logger.debug { "Mottatt avregningsgrunnlag fra UR. Meldingsinnhold: $jmsMessage" }
            val avregningsgrunnlagWrapper = json.decodeFromString<AvregningsgrunnlagWrapper>(jmsMessage)
            processAvregningsgrunnlagMessage(avregningsgrunnlagWrapper.avregningsgrunnlag)
            message.acknowledge()
        }.onFailure { exception ->
            secureLogger.error { "Avregningsgrunnlagmelding fra UR: $jmsMessage" }
            logger.error(exception) { "Prosessering av avregningsgrunnlag feilet. jmsMessageID: ${message.jmsMessageID}" }
        }
    }

    private fun processAvregningsgrunnlagMessage(avregningsgrunnlag: Avregningsgrunnlag) {
        val avregningstransaksjon: Avregningstransaksjon? =
            when {
                avregningsgrunnlag.delytelseId != null -> {
                    transaksjonRepository.findTransaksjonByMotIdAndPersonIdAndTomDato(
                        avregningsgrunnlag.delytelseId,
                        avregningsgrunnlag.fagSystemId.stringToInt(),
                        avregningsgrunnlag.tomdato.toLocalDate()!!,
                    )
                }

                avregningsgrunnlag.trekkvedtakId != null -> {
                    transaksjonRepository.findTransaksjonByTrekkvedtakId(avregningsgrunnlag.trekkvedtakId)
                }

                else -> null
            }

        val avregningsretur: Avregningsretur? =
            avregningstransaksjon?.let {
                avregningsgrunnlag.toAvregningsretur(it)
            } ?: avregningsgrunnlag.trekkvedtakId?.let {
                setKreditorRefToTransEksId(avregningsgrunnlag)
            }

        avregningsretur?.apply {
            datoAvsender = datoAvsender ?: "1900-01-01".toIsoDate()
        }?.let {
            dataSource.transaction { session ->
                avregningsreturRepository.insert(it, session)
            }
        } ?: throw NullPointerException("Feilet i prosessering av avregningsgrunnlag: avregningsretur er null")
    }

    private fun setKreditorRefToTransEksId(avregningsgrunnlag: Avregningsgrunnlag): Avregningsretur {
        return avregningsgrunnlag.toAvregningsretur(
            avregningstransaksjon =
                Avregningstransaksjon(
                    transEksId = avregningsgrunnlag.kreditorRef,
                    datoAnviser = "1900-01-01".toIsoDate(),
                ),
        )
    }
}
