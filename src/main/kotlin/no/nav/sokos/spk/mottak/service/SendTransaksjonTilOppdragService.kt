package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
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

class SendTransaksjonTilOppdragService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val outboxRepository: OutboxRepository = OutboxRepository(dataSource),
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
    fun hentTransaksjonOgSendTilOppdrag() {
        val timer = Instant.now()
        var transaksjoner: Map<Int, String> = emptyMap()
        dataSource.transaction { session ->
            runCatching {
                transaksjoner = getTransaksjoner(session)
                producer.send(transaksjoner.entries.map { it.value })
                deleteTransaksjoner(transaksjoner.keys, session)
            }.onFailure { exception ->
                when (exception) {
                    is MottakException -> {
                        logger.error { "Feiler ved utsendelse av ${transaksjoner.size} transaksjoner til OppdragZ: $exception" }
                    }

                    else -> {
                        logger.error { "Feiler ved db-uthenting av ${transaksjoner.size} transaksjoner til OppdragZ: $exception" }
                    }
                }
            }
            logger.info { "Fullført sending av ${transaksjoner.size} transaksjoner til OppdragZ. Total tid: ${Duration.between(timer, Instant.now()).toSeconds()} sekunder." }
        }
    }

    fun getTransaksjoner(session: Session): Map<Int, String> {
        return outboxRepository.get(session)?.toMap() ?: return emptyMap()
    }

    fun deleteTransaksjoner(
        transaksjonIdList: Set<Int>,
        session: Session,
    ) {
        outboxRepository.delete(transaksjonIdList, session)
    }
}
