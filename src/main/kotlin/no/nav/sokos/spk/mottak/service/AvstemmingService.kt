package no.nav.sokos.spk.mottak.service

import com.ibm.mq.jakarta.jms.MQQueue
import com.ibm.msg.client.jakarta.wmq.WMQConstants
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_AVSTEMMING
import no.nav.sokos.spk.mottak.domain.TransaksjonDetalj
import no.nav.sokos.spk.mottak.domain.TransaksjonOppsummering
import no.nav.sokos.spk.mottak.domain.converter.AvstemmingConverter
import no.nav.sokos.spk.mottak.domain.converter.AvstemmingConverter.avvikMelding
import no.nav.sokos.spk.mottak.domain.converter.AvstemmingConverter.dataMelding
import no.nav.sokos.spk.mottak.domain.converter.AvstemmingConverter.sluttMelding
import no.nav.sokos.spk.mottak.domain.converter.AvstemmingConverter.startMelding
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.metrics.SERVICE_CALL
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.repository.FilInfoRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.util.JaxbUtils
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata

private val logger = KotlinLogging.logger { }
private const val ANTALL_DETALJER_PER_MELDING = 70
private const val ANTALL_IKKE_UTFORT_TRANSAKSJON = 500

class AvstemmingService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val filInfoRepository: FilInfoRepository = FilInfoRepository(dataSource),
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val producer: JmsProducerService =
        JmsProducerService(
            senderQueue =
                MQQueue(PropertiesConfig.MQProperties().avstemmingQueueName).apply {
                    targetClient = WMQConstants.WMQ_CLIENT_NONJMS_MQ
                },
            metricCounter = Metrics.mqUtbetalingProducerMetricCounter,
        ),
) {
    fun sendGrensesnittAvstemming() {
        runCatching {
            val fileInfoMap = filInfoRepository.getByAvstemmingStatusIsOSO(ANTALL_IKKE_UTFORT_TRANSAKSJON)

            if (fileInfoMap.isNotEmpty()) {
                Metrics.timer(SERVICE_CALL, "AvstemmingService", "sendGrensesnittAvstemming").recordCallable {
                    val oppsummeringMap =
                        fileInfoMap
                            .flatMap { transaksjonRepository.findTransaksjonOppsummeringByFilInfoId(it.key) }
                            .groupBy { it.fagomrade }
                    logger.debug { "Transaksjonsoppsummering: $oppsummeringMap" }

                    val filInfoIdList = fileInfoMap.map { it.key }
                    val transaksjonDetaljer = transaksjonRepository.findTransaksjonDetaljerByFilInfoId(filInfoIdList)
                    oppsummeringMap.forEach { oppsummering -> sendAvstemmingTilMQ(oppsummering.key, oppsummering.value, transaksjonDetaljer, filInfoIdList) }
                    dataSource.transaction { session ->
                        filInfoRepository.updateAvstemmingStatus(filInfoIdList, TRANS_TILSTAND_OPPDRAG_AVSTEMMING, session)
                    }
                    logger.info { "Avstemming sendt OK for filInfoId: ${filInfoIdList.joinToString()}" }
                }
            } else {
                logger.info { "Ingen transaksjoner eller for mange transaksjoner som ikke har fått kvittering fra OppdragZ" }
            }
        }.onFailure { exception ->
            val errorMessage = "Utsending av avstemming til OppdragZ feilet. Feilmelding: ${exception.message}"
            logger.error(exception) { errorMessage }
            throw MottakException(errorMessage)
        }
    }

    private fun sendAvstemmingTilMQ(
        fagomrade: String,
        oppsummering: List<TransaksjonOppsummering>,
        transaksjonDetaljer: List<TransaksjonDetalj>,
        filInfoIdList: List<Int>,
    ) {
        val avstemming = AvstemmingConverter.default(filInfoIdList.first().toString(), filInfoIdList.last().toString(), fagomrade)
        val avstemmingList = mutableListOf<Avstemmingsdata>()
        avstemmingList.add(avstemming.startMelding())

        if (transaksjonDetaljer.isNotEmpty()) {
            transaksjonDetaljer.chunked(ANTALL_DETALJER_PER_MELDING).forEach {
                avstemmingList.add(avstemming.avvikMelding(it))
            }
        }

        avstemmingList.add(avstemming.dataMelding(oppsummering))
        avstemmingList.add(avstemming.sluttMelding())
        avstemmingList.map { JaxbUtils.marshallAvstemmingsdata(it) }.run { producer.send(this) }
    }
}
