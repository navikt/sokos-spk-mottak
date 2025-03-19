package no.nav.sokos.spk.mottak.service

import java.time.Duration
import java.time.Instant

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.MQ_BATCH_SIZE
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.converter.TrekkConverter.innrapporteringTrekk
import no.nav.sokos.spk.mottak.exception.DatabaseException
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.metrics.SERVICE_CALL
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

private val logger = KotlinLogging.logger { }

class SendTrekkService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource),
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource),
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
        if (innTransaksjonRepository.countByInnTransaksjon() > 0) {
            logger.info { "Eksisterer innTransaksjoner som ikke er ferdig behandlet og derfor blir ingen trekktransaksjoner behandlet" }
            return
        }

        val timer = Instant.now()
        val transaksjoner = getTransaksjoner() ?: return
        Metrics.timer(SERVICE_CALL, "SendTrekkTransaksjonToOppdragZService", "getTrekkTransaksjonAndSendToOppdrag").recordCallable {
            logger.info { "Starter sending av ${transaksjoner.size} trekktransaksjoner til OppdragZ" }
            val totalTransaksjoner = processTransaksjoner(transaksjoner, mqBatchSize)
            logger.info { "Fullført sending av ${transaksjoner.size} trekktransaksjoner til OppdragZ. Total tid: ${Duration.between(timer, Instant.now()).toSeconds()} sekunder." }
            Metrics.trekkTransaksjonerTilOppdragCounter.inc(totalTransaksjoner.toLong())
        }
    }

    private fun getTransaksjoner(): List<Transaksjon>? =
        runCatching {
            transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
        }.onFailure { databaseException ->
            val errorMessage = "Db2-feil: + ${databaseException.message}"
            logger.error(databaseException) { errorMessage }
            throw DatabaseException(errorMessage, databaseException)
        }.getOrNull()

    private fun processTransaksjoner(
        transaksjoner: List<Transaksjon>,
        mqBatchSize: Int,
    ): Int {
        var totalTransaksjoner = 0
        transaksjoner.chunked(mqBatchSize).forEach { chunk ->
            val transaksjonIdList = chunk.mapNotNull { it.transaksjonId }
            runCatching {
                dataSource.transaction { session ->
                    val trekkMeldinger = chunk.map { it.innrapporteringTrekk() }
                    producer.send(trekkMeldinger)
                    updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_OK, session)
                    totalTransaksjoner += transaksjonIdList.size
                }
            }.onFailure { exception ->
                logger.error(exception) { "Utsending av trekktransaksjonene feilet. Feilmelding: ${exception.message}" }
                if (exception is MottakException) {
                    runCatching {
                        dataSource.transaction { session ->
                            updateTransaksjonAndTransaksjonTilstand(transaksjonIdList, TRANS_TILSTAND_TREKK_SENDT_FEIL, session)
                        }
                    }.onFailure { databaseException ->
                        logger.error(databaseException) { "Db2-feil: + ${databaseException.message}" }
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
