package no.nav.sokos.spk.mottak.mq

import com.ibm.mq.jakarta.jms.MQQueue
import com.zaxxer.hikari.HikariDataSource
import jakarta.jms.ConnectionFactory
import jakarta.jms.Message
import jakarta.jms.Queue
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SECURE_LOGGER
import no.nav.sokos.spk.mottak.domain.OS_STATUS_OK
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_OK
import no.nav.sokos.spk.mottak.domain.UTBETALING_LISTENER_SERVICE
import no.nav.sokos.spk.mottak.metrics.Metrics.mqUtbetalingListenerMetricCounter
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER)

class UtbetalingListenerService(
    connectionFactory: ConnectionFactory = MQConfig.connectionFactory(),
    utbetalingReplyQueue: Queue = MQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) : JmsListener(connectionFactory) {
    private val utbetalingMQListener = session.createConsumer(utbetalingReplyQueue)
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource)
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    init {
        utbetalingMQListener.setMessageListener { onUtbetalingMessage(it) }
    }

    private fun onUtbetalingMessage(message: Message) {
        val jmsMessage = message.getBody(String::class.java)
        runCatching {
            logger.debug { "Mottatt oppdragsmeldingretur fra OppdragZ. Meldingsinnhold: $jmsMessage" }
            val oppdrag = JaxbUtils.unmarshallOppdrag(jmsMessage)

            val transTilstandStatus =
                when {
                    oppdrag.mmel.alvorlighetsgrad.toInt() < 5 -> TRANS_TILSTAND_OPPDRAG_RETUR_OK
                    else -> {
                        logger.error { "Prosessering av returmelding feilet med alvorlighetsgrad ${oppdrag.mmel.alvorlighetsgrad}. Feilmelding: ${message.jmsMessageID}" }
                        TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
                    }
                }

            val transaksjonIdList =
                oppdrag.oppdrag110.oppdragsLinje150
                    .filter { !isDuplicate(it.delytelseId.toInt(), oppdrag.mmel.alvorlighetsgrad) }
                    .map { it.delytelseId.toInt() }

            if (transaksjonIdList.isEmpty()) {
                logger.info { "Ingen nye oppdragsmeldingreturer å prosessere" }
                return
            }

            dataSource.transaction { session ->
                val transTilstandIdList =
                    transaksjonTilstandRepository.insertBatch(
                        transaksjonIdList = transaksjonIdList,
                        transaksjonTilstandType = transTilstandStatus,
                        systemId = UTBETALING_LISTENER_SERVICE,
                        feilkode = oppdrag.mmel.kodeMelding,
                        feilkodeMelding = oppdrag.mmel.beskrMelding,
                        session = session,
                    )

                transaksjonRepository.updateBatch(
                    transaksjonIdList = transaksjonIdList,
                    transTilstandIdList = transTilstandIdList,
                    transaksjonTilstandType = transTilstandStatus,
                    systemId = UTBETALING_LISTENER_SERVICE,
                    osStatus = oppdrag.mmel.alvorlighetsgrad,
                    session = session,
                )
            }
            mqUtbetalingListenerMetricCounter.inc(transaksjonIdList.size.toLong())
            message.acknowledge()
        }.onFailure { exception ->
            secureLogger.error { "Utbetalingsmelding fra OppdragZ: $jmsMessage" }
            logger.error(exception) { "Prosessering av utbetalingsmeldingretur feilet. ${message.jmsMessageID}" }
            session.rollback()
        }
    }

    private fun isDuplicate(
        transaksjonId: Int,
        osStatus: String,
    ): Boolean {
        return when {
            osStatus == OS_STATUS_OK -> false
            transaksjonRepository.getByTransaksjonId(transaksjonId)!!.osStatus == TRANSAKSJONSTATUS_OK -> {
                logger.info { "Transaksjon: $transaksjonId er allerede mottatt med OK-status" }
                true
            }

            else -> false
        }
    }
}
