package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQ_BATCH_SIZE
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.converter.TrekkConverter.innrapporteringTrekk
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.metrics.SERVICE_CALL
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import java.time.Duration
import java.time.Instant

private val logger = KotlinLogging.logger { }

class SendTrekkTransaksjonToOppdragZService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource),
    private val mqBatchSize: Int = MQ_BATCH_SIZE,
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
    fun getTrekkTransaksjonAndSendToOppdrag() {
        val transaksjoner = getTransaksjoner() ?: return
        Metrics.timer(SERVICE_CALL, "SendTrekkTransaksjonToOppdragZService", "getTrekkTransaksjonAndSendToOppdrag").recordCallable {
            logger.info { "Starter sending av ${transaksjoner.size} trekktransaksjoner til OppdragZ" }
            val totalTransaksjoner = processTransaksjoner(transaksjoner, mqBatchSize)
            Metrics.trekkTransaksjonerTilOppdragCounter.inc(totalTransaksjoner.toLong())
        }
    }

    private fun getTransaksjoner(): List<Transaksjon>? =
        runCatching {
            transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
        }.onFailure { exception ->
            logger.error(exception) { "Fatal feil ved henting av trekktransaksjoner: ${exception.message}" }
            throw RuntimeException("Fatal feil ved henting av trekktransaksjoner")
        }.getOrNull()

    private fun processTransaksjoner(
        transaksjoner: List<Transaksjon>,
        mqBatchSize: Int,
    ): Int {
        val timer = Instant.now()
        var totalTransaksjoner = 0
        transaksjoner.chunked(mqBatchSize).forEach { chunk ->
            val transaksjonIdList = chunk.mapNotNull { it.transaksjonId }
            runCatching {
                dataSource.transaction { session ->
                    val trekkMeldinger = chunk.map { JaxbUtils.marshallTrekk(it.innrapporteringTrekk()) }
                    producer.send(trekkMeldinger)
                    updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_OK, session)
                    totalTransaksjoner += transaksjonIdList.size
                    logger.info { "Fullført sending av ${transaksjonIdList.size} trekktransaksjoner til OppdragZ. Total tid: ${Duration.between(timer, Instant.now()).toSeconds()} sekunder." }
                }
            }.onFailure { exception ->
                logger.error(exception) { "Feiler ved utsending av trekktransaksjonene: ${transaksjonIdList.joinToString()} : $exception" }
                if (exception is MottakException) {
                    runCatching {
                        dataSource.transaction { session ->
                            updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_FEIL, session)
                        }
                    }.onFailure { exception ->
                        logger.error(exception) { "DB2-feil: $exception" }
                    }
                }
            }
        }
        return totalTransaksjoner
    }

    private fun updateTransaksjonAndTransaksjonTilstand(
        transaksjonIdList: List<Int>,
        transTilstandStatus: String,
        session: Session,
    ) {
        val transaksjonTilstandIdList = transaksjonTilstandRepository.insertBatch(transaksjonIdList, transTilstandStatus, session = session)
        transaksjonRepository.updateBatch(transaksjonIdList, transaksjonTilstandIdList, transTilstandStatus, session = session)
    }
}
