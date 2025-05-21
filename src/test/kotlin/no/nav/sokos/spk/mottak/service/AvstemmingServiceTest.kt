package no.nav.sokos.spk.mottak.service

import java.io.StringReader
import java.sql.SQLException
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

import kotlinx.datetime.LocalDate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import jakarta.xml.bind.JAXBContext
import kotliquery.queryOf
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.api.model.AvstemmingRequest
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_IKKE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_TREKK
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.MOT
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_AVSTEMMING_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_OK
import no.nav.sokos.spk.mottak.domain.converter.AVLEVERENDE_KOMPONENT_KODE
import no.nav.sokos.spk.mottak.domain.converter.MOTTAKENDE_KOMPONENT_KODE
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.metrics.Metrics.mqUtbetalingProducerMetricCounter
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AvstemmingType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.DetaljType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.KildeType

internal class AvstemmingServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, MQListener))

        val producer: JmsProducerService by lazy {
            spyk(
                JmsProducerService(
                    senderQueue = ActiveMQQueue(PropertiesConfig.MQProperties().avstemmingQueueName),
                    metricCounter = mqUtbetalingProducerMetricCounter,
                    connectionFactory = connectionFactory,
                ),
            )
        }

        val avstemmingService: AvstemmingService by lazy {
            AvstemmingService(
                dataSource = Db2Listener.dataSource,
                filInfoRepository = Db2Listener.filInfoRepository,
                transaksjonRepository = Db2Listener.transaksjonRepository,
                producer = producer,
            )
        }

        Given("det fins transaksjoner som er klar til avstemming ") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/avstemming_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository
                .findAllByBelopstypeAndByTransaksjonTilstand(
                    listOf(BELOPSTYPE_SKATTEPLIKTIG_UTBETALING, BELOPSTYPE_IKKE_SKATTEPLIKTIG_UTBETALING, BELOPSTYPE_TREKK),
                    listOf(TRANS_TILSTAND_OPPDRAG_RETUR_OK, TRANS_TILSTAND_OPPDRAG_RETUR_FEIL, TRANS_TILSTAND_OPPDRAG_SENDT_OK),
                ).size shouldBe 11
            Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_GOD, listOf("000034")).first().avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK

            val avstemmingSlot = slot<List<String>>()
            every { producer.send(capture(avstemmingSlot)) } answers { callOriginal() }

            When("kall grensesnitt avstemming til OppdragZ") {
                avstemmingService.sendGrensesnittAvstemming(AvstemmingRequest(null, null))

                Then("skal det sendes grensesnitt avstemming til OppdragZ og filInfo skal bli oppdatert med 'OAO'") {
                    Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_GOD, listOf("000034")).first().avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_AVSTEMMING_OK
                    val avstemmingsdataList = avstemmingSlot.captured.map { unmarshallAvstemming(it) }

                    avstemmingsdataList.size shouldBe 4
                    verifyAvstemmingsdata(avstemmingsdataList.first(), AksjonType.START)
                    verifyAvstemmingsdata(avstemmingsdataList[1], AksjonType.DATA)
                    avstemmingsdataList[1].detalj.size shouldBe 4
                    avstemmingsdataList[1].detalj.filter { it.detaljType == DetaljType.AVVI }.size shouldBe 2
                    avstemmingsdataList[1].detalj.filter { it.detaljType == DetaljType.MANG }.size shouldBe 1
                    avstemmingsdataList[1].detalj.filter { it.detaljType == DetaljType.VARS }.size shouldBe 1

                    verifyAvstemmingsdata(avstemmingsdataList[2], AksjonType.DATA)
                    avstemmingsdataList[2].total.totalAntall shouldBe 7
                    avstemmingsdataList[2].grunnlag.godkjentAntall shouldBe 4
                    avstemmingsdataList[2].grunnlag.varselAntall shouldBe 1
                    avstemmingsdataList[2].grunnlag.avvistAntall shouldBe 2
                    verifyAvstemmingsdata(avstemmingsdataList.last(), AksjonType.AVSL)
                }
            }
        }

        Given("det fins transaksjoner som er klar til avstemming med AvstemmingRequest ") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/avstemming_transaksjon_dato.sql")))
            }
            Db2Listener.transaksjonRepository
                .findAllByBelopstypeAndByTransaksjonTilstand(
                    listOf(BELOPSTYPE_SKATTEPLIKTIG_UTBETALING, BELOPSTYPE_IKKE_SKATTEPLIKTIG_UTBETALING, BELOPSTYPE_TREKK),
                    listOf(TRANS_TILSTAND_OPPDRAG_RETUR_OK, TRANS_TILSTAND_OPPDRAG_RETUR_FEIL, TRANS_TILSTAND_OPPDRAG_SENDT_OK),
                ).size shouldBe 10
            Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(filTilstandType = null, lopeNummer = emptyList()).size shouldBe 4

            val avstemmingSlot = slot<List<String>>()
            every { producer.send(capture(avstemmingSlot)) } answers { callOriginal() }

            When("kall grensesnitt avstemming til OppdragZ") {
                avstemmingService.sendGrensesnittAvstemming(AvstemmingRequest(LocalDate(2024, 2, 1), LocalDate(2024, 2, 2)))

                Then("skal det sendes grensesnitt avstemming til OppdragZ og filInfo skal bli oppdatert med 'OAO'") {
                    Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(filTilstandType = null, lopeNummer = emptyList()).forEach {
                        if (listOf(20000816, 20000818, 20000819).contains(it.filInfoId)) {
                            it.avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK
                        } else {
                            it.avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_AVSTEMMING_OK
                        }
                    }
                    val avstemmingsdataList = avstemmingSlot.captured.map { unmarshallAvstemming(it) }

                    avstemmingsdataList.size shouldBe 4
                    verifyAvstemmingsdata(avstemmingsdataList.first(), AksjonType.START)
                    verifyAvstemmingsdata(avstemmingsdataList[1], AksjonType.DATA)
                    avstemmingsdataList[1].detalj.size shouldBe 2
                    avstemmingsdataList[1].detalj.filter { it.detaljType == DetaljType.AVVI }.size shouldBe 2
                    avstemmingsdataList[1].detalj.filter { it.detaljType == DetaljType.MANG }.size shouldBe 0
                    avstemmingsdataList[1].detalj.filter { it.detaljType == DetaljType.VARS }.size shouldBe 0
                    verifyAvstemmingsdata(avstemmingsdataList[2], AksjonType.DATA)
                    avstemmingsdataList[2].total.totalAntall shouldBe 3
                    avstemmingsdataList[2].grunnlag.godkjentAntall shouldBe 1
                    avstemmingsdataList[2].grunnlag.varselAntall shouldBe 0
                    avstemmingsdataList[2].grunnlag.avvistAntall shouldBe 2
                    verifyAvstemmingsdata(avstemmingsdataList.last(), AksjonType.AVSL)
                }
            }
        }

        Given("det fins transaksjoner som er klar til avstemming og MQ server er nede") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/avstemming_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository
                .findAllByBelopstypeAndByTransaksjonTilstand(
                    listOf(BELOPSTYPE_SKATTEPLIKTIG_UTBETALING, BELOPSTYPE_IKKE_SKATTEPLIKTIG_UTBETALING, BELOPSTYPE_TREKK),
                    listOf(TRANS_TILSTAND_OPPDRAG_RETUR_OK, TRANS_TILSTAND_OPPDRAG_RETUR_FEIL, TRANS_TILSTAND_OPPDRAG_SENDT_OK),
                ).size shouldBe 11
            Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_GOD, listOf("000034")).first().avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK
            every { Db2Listener.filInfoRepository.getByAvstemmingStatus(any()) } throws SQLException("No database connection!")

            When("kall grensesnitt avstemming til OppdragZ") {
                val exception = shouldThrow<MottakException> { avstemmingService.sendGrensesnittAvstemming(AvstemmingRequest(null, null)) }

                Then("skal det ikke sendes grensesnitt avstemming til OppdragZ og filInfo skal ikke bli oppdatert") {
                    exception.message shouldBe "Utsending av avstemming til OppdragZ feilet. Feilmelding: No database connection!"
                    Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_GOD, listOf("000034")).first().avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK
                }
            }
        }
    })

private fun unmarshallAvstemming(xml: String): Avstemmingsdata =
    JAXBContext
        .newInstance(Avstemmingsdata::class.java)
        .createUnmarshaller()
        .unmarshal(
            XMLInputFactory.newInstance().createXMLStreamReader(StreamSource(StringReader(xml))),
            Avstemmingsdata::class.java,
        ).value

private fun verifyAvstemmingsdata(
    avstemmingsdata: Avstemmingsdata,
    aksjonType: AksjonType,
) {
    avstemmingsdata.aksjon.aksjonType shouldBe aksjonType
    avstemmingsdata.aksjon.kildeType shouldBe KildeType.AVLEV
    avstemmingsdata.aksjon.avstemmingType shouldBe AvstemmingType.GRSN
    avstemmingsdata.aksjon.avleverendeKomponentKode shouldBe AVLEVERENDE_KOMPONENT_KODE
    avstemmingsdata.aksjon.mottakendeKomponentKode shouldBe MOTTAKENDE_KOMPONENT_KODE
    avstemmingsdata.aksjon.underkomponentKode shouldBe "PENSPK"
    avstemmingsdata.aksjon.nokkelFom shouldNotBe null
    avstemmingsdata.aksjon.nokkelFom shouldNotBe null
    avstemmingsdata.aksjon.avleverendeAvstemmingId shouldNotBe null
    avstemmingsdata.aksjon.brukerId = MOT

    if (aksjonType == AksjonType.DATA) {
        when {
            avstemmingsdata.detalj.isNotEmpty() -> {
                avstemmingsdata.total shouldBe null
                avstemmingsdata.periode shouldBe null
                avstemmingsdata.grunnlag shouldBe null
            }

            else -> {
                avstemmingsdata.total shouldNotBe null
                avstemmingsdata.periode shouldNotBe null
                avstemmingsdata.grunnlag shouldNotBe null
            }
        }
    }
}
