package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.repository.OutboxRepository
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

class SendUtbetalingService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val outboxRepository: OutboxRepository = OutboxRepository(),
    private val producer: JmsProducerService =
        JmsProducerService(
            MQQueue(PropertiesConfig.MQProperties().utbetalingQueueName).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            },
            MQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            },
            Metrics.mqUtbetalingProducerMetricCounter,
        ),
) {
    fun sendToOppdrag() {
        val timer = Instant.now()
        var transaksjoner = emptyList<Pair<String, String>>()
        dataSource.transaction { session ->
            runCatching {
                transaksjoner = outboxRepository.getUtbetaling(session)
                if (transaksjoner.isEmpty()) {
                    return@transaction
                }
                producer.send(transaksjoner.map { it.second })
                outboxRepository.deleteUtbetaling(transaksjoner.map { it.first }, session)
                logger.info { "Fullført sending av ${transaksjoner.size} utbetalingstransaksjoner til OppdragZ. Total tid: ${Duration.between(timer, Instant.now()).toSeconds()} sekunder." }
            }.onFailure { exception ->
                when (exception) {
                    is MottakException -> {
                        logger.error { "Feiler ved utsendelse av ${transaksjoner.size} utbetalingstransaksjoner til OppdragZ: $exception" }
                    }

                    else -> {
                        logger.error { "Feiler ved db-uthenting av ${transaksjoner.size} utbetalingstransaksjoner til OppdragZ: $exception" }
                    }
                }
            }
        }
    }
}
