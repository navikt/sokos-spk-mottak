package no.nav.sokos.spk.mottak.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.slot
import io.mockk.spyk
import jakarta.xml.bind.JAXBContext
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_IKKE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_TREKK
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.MOT
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_AVSTEMMING
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
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AvstemmingType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.KildeType
import org.apache.activemq.artemis.jms.client.ActiveMQQueue
import java.io.StringReader
import java.sql.SQLException
import javax.xml.stream.XMLInputFactory
import javax.xml.transform.stream.StreamSource

class AvstemmingServiceTest :
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
                ).size shouldBe 10
            Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(34, FILTILSTANDTYPE_GOD)?.avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK

            val avstemmingSlot = slot<List<String>>()
            every { producer.send(capture(avstemmingSlot)) } answers { callOriginal() }

            When("kall grensesnitt avstemming til OppdragZ") {
                avstemmingService.sendGrensesnittAvstemming()

                Then("skal det sendes grensesnitt avstemming til OppdragZ og filInfo skal bli oppdatert med 'OAO'") {
                    Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(34, FILTILSTANDTYPE_GOD)?.avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_AVSTEMMING
                    val avstemmingsdataList = avstemmingSlot.captured.map { unmarshallAvstemming(it) }

                    avstemmingsdataList.size shouldBe 4
                    verifyAvstemmingsdata(avstemmingsdataList[0], AksjonType.START)
                    verifyAvstemmingsdata(avstemmingsdataList[1], AksjonType.DATA)
                    verifyAvstemmingsdata(avstemmingsdataList[2], AksjonType.DATA)
                    verifyAvstemmingsdata(avstemmingsdataList[3], AksjonType.AVSL)
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
                ).size shouldBe 10
            Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(34, FILTILSTANDTYPE_GOD)?.avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK
            every { Db2Listener.filInfoRepository.getByAvstemmingStatusIsOSO(any()) } throws SQLException("No database connection!")

            When("kall grensesnitt avstemming til OppdragZ") {
                val exception = shouldThrow<MottakException> { avstemmingService.sendGrensesnittAvstemming() }

                Then("skal det ikke sendes grensesnitt avstemming til OppdragZ og filInfo skal ikke bli oppdatert") {
                    exception.message shouldBe "Utsending av avstemming til OppdragZ feilet. Feilmelding: No database connection!"
                    Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(34, FILTILSTANDTYPE_GOD)?.avstemmingStatus shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK
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
            avstemmingsdata.detalj.isNotEmpty() -> avstemmingsdata.detalj.size shouldBe 3
            else -> {
                avstemmingsdata.total shouldNotBe null
                avstemmingsdata.periode shouldNotBe null
                avstemmingsdata.grunnlag shouldNotBe null
            }
        }
    }
}
