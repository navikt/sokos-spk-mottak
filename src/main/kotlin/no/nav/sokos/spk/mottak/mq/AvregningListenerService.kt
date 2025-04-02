package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import jakarta.jms.ConnectionFactory
import jakarta.jms.Message
import jakarta.jms.Queue
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SECURE_LOGGER
import no.nav.sokos.spk.mottak.domain.Avregningsgrunnlag
import no.nav.sokos.spk.mottak.domain.AvregningsgrunnlagWrapper
import no.nav.sokos.spk.mottak.domain.Avregningsretur
import no.nav.sokos.spk.mottak.domain.toAvregningsAvvik
import no.nav.sokos.spk.mottak.domain.toAvregningsretur
import no.nav.sokos.spk.mottak.dto.Avregningstransaksjon
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.AvregningsavvikRepository
import no.nav.sokos.spk.mottak.repository.AvregningsreturRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction
import no.nav.sokos.spk.mottak.util.Utils.toIsoDate
import no.nav.sokos.spk.mottak.util.Utils.toLocalDate

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER)
const val UNKNOWN_TRANSACTION_DATE = "1900-01-01"

class AvregningListenerService(
    connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
    avregningsgrunnlagQueue: Queue = MQQueue(PropertiesConfig.MQProperties().avregningsgrunnlagQueueName),
    private val producer: JmsProducerService =
        JmsProducerService(
            senderQueue =
                MQQueue(PropertiesConfig.MQProperties().avregningsgrunnlagQueueName + "_BOQ").apply {
                    targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
                },
            metricCounter = Metrics.mqAvregningBOQListenerMetricCounter,
        ),
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) : JmsListener(connectionFactory) {
    private val avregningsgrunnlagMQListener = jmsContext.createConsumer(avregningsgrunnlagQueue)
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val avregningsreturRepository: AvregningsreturRepository = AvregningsreturRepository(dataSource)
    private val avregningsavvikRepository: AvregningsavvikRepository = AvregningsavvikRepository(dataSource)

    init {
        avregningsgrunnlagMQListener.setMessageListener { onAvregningsgrunnlagMessage(it) }
    }

    private fun onAvregningsgrunnlagMessage(message: Message) {
        val jmsMessage = message.getBody(String::class.java)
        var avregningsgrunnlagWrapper: AvregningsgrunnlagWrapper? = null
        runCatching {
            secureLogger.debug { "Mottatt avregningsgrunnlag fra UR. Meldingsinnhold: $jmsMessage" }
            avregningsgrunnlagWrapper = json.decodeFromString<AvregningsgrunnlagWrapper>(jmsMessage)
            processAvregningsgrunnlagMessage(avregningsgrunnlagWrapper!!.avregningsgrunnlag)
            message.acknowledge()
        }.onFailure { exception ->
            secureLogger.error { "Avregningsgrunnlagmelding fra UR: $jmsMessage" }

            avregningsgrunnlagWrapper?.let { wrapper ->
                logger.error(exception) {
                    "Prosessering av avregningsgrunnlag feilet: ${exception.message}, " +
                        "jmsMessageID: ${message.jmsMessageID}, " +
                        "oppdragsId: ${wrapper.avregningsgrunnlag.oppdragsId}, " +
                        "oppdragsLinjeId: ${wrapper.avregningsgrunnlag.linjeId}, " +
                        "trekkvedtakId: ${wrapper.avregningsgrunnlag.trekkvedtakId}, " +
                        "bilagsnrSerie: ${wrapper.avregningsgrunnlag.bilagsnrSerie}, " +
                        "bilagsnr: ${wrapper.avregningsgrunnlag.bilagsnr}, " +
                        "delytelseId: ${wrapper.avregningsgrunnlag.delytelseId} " +
                        "fagSystemId: ${wrapper.avregningsgrunnlag.fagSystemId}"
                }
                wrapper.avregningsgrunnlag.let { avregningsgrunnlag ->
                    runCatching {
                        dataSource.transaction { session ->
                            avregningsavvikRepository.insert(
                                avregningsgrunnlag.toAvregningsAvvik(),
                                exception.message!!,
                                session,
                            )
                        }
                    }.onFailure { ex ->
                        logger.error(ex) { "Feiler ved lagring av avviksmelding i T_AVREGNING_AVVIK: ${ex.message}" }
                    }
                }
            }
            producer.send(listOf(jmsMessage))
            message.acknowledge()
        }
    }

    private fun processAvregningsgrunnlagMessage(avregningsgrunnlag: Avregningsgrunnlag) {
        val avregningstransaksjon = findTransaksjon(avregningsgrunnlag)
        val avregningsretur = createAvregningsretur(avregningsgrunnlag, avregningstransaksjon)
        dataSource.transaction { session ->
            avregningsreturRepository.insert(avregningsretur, session)
        }
        Metrics.mqAvregningListenerMetricCounter.inc()
    }

    private fun findTransaksjon(avregningsgrunnlag: Avregningsgrunnlag): Avregningstransaksjon? {
        return when {
            !avregningsgrunnlag.delytelseId.isNullOrBlank() && avregningsgrunnlag.fagSystemId.toIntOrNull() != null -> {
                avregningsgrunnlag.tomdato.toLocalDate()?.let { tomDato ->
                    transaksjonRepository.findTransaksjonByMotIdAndPersonIdAndTomDato(
                        avregningsgrunnlag.delytelseId,
                        avregningsgrunnlag.fagSystemId.toInt(),
                        tomDato,
                    )
                }
            }

            avregningsgrunnlag.trekkvedtakId != null -> {
                transaksjonRepository.findTransaksjonByTrekkvedtakId(avregningsgrunnlag.trekkvedtakId)
            }

            else -> null
        }
    }

    private fun createAvregningsretur(
        avregningsgrunnlag: Avregningsgrunnlag,
        avregningstransaksjon: Avregningstransaksjon?,
    ): Avregningsretur {
        return avregningstransaksjon?.let {
            avregningsgrunnlag.toAvregningsretur(it)
        } ?: avregningsgrunnlag.trekkvedtakId?.let {
            setKreditorRefToTransEksId(avregningsgrunnlag)
        } ?: setUnknownAvregningsretur(avregningsgrunnlag)
    }

    private fun setUnknownAvregningsretur(avregningsgrunnlag: Avregningsgrunnlag): Avregningsretur {
        return avregningsgrunnlag.toAvregningsretur(
            Avregningstransaksjon(
                datoAnviser = UNKNOWN_TRANSACTION_DATE.toIsoDate(),
            ),
        )
    }

    private fun setKreditorRefToTransEksId(avregningsgrunnlag: Avregningsgrunnlag): Avregningsretur {
        return avregningsgrunnlag.toAvregningsretur(
            Avregningstransaksjon(
                transEksId = avregningsgrunnlag.kreditorRef,
                datoAnviser = UNKNOWN_TRANSACTION_DATE.toIsoDate(),
            ),
        )
    }
}
