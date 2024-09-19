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

class SendTrekkService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val outboxRepository: OutboxRepository = OutboxRepository(),
    private val producer: JmsProducerService =
        JmsProducerService(
            MQQueue(PropertiesConfig.MQProperties().trekkQueueName).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            },
            MQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName).apply {
                targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
            },
            Metrics.mqTrekkProducerMetricCounter,
        ),
) {
    fun sendToOppdrag() {
        val timer = Instant.now()
        var transaksjoner = emptyList<Pair<Int, String>>()
        dataSource.transaction { session ->
            runCatching {
                transaksjoner = outboxRepository.getTrekk(session)
                if (transaksjoner.isEmpty()) {
                    return@transaction
                }
                producer.send(transaksjoner.map { it.second })
                outboxRepository.deleteTrekk(transaksjoner.map { it.first }, session)
                logger.info { "Fullført sending av ${transaksjoner.size} trekktransaksjoner til OppdragZ. Total tid: ${Duration.between(timer, Instant.now()).toSeconds()} sekunder." }
            }.onFailure { exception ->
                when (exception) {
                    is MottakException -> {
                        logger.error { "Feiler ved utsendelse av ${transaksjoner.size} trekktransaksjoner til OppdragZ: $exception" }
                    }

                    else -> {
                        logger.error { "Feiler ved db-uthenting av ${transaksjoner.size} trekktransaksjoner til OppdragZ: $exception" }
                    }
                }
            }
        }
    }
}
