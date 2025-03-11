package no.nav.sokos.spk.mottak.mq

import java.time.LocalDate

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.AvregningsgrunnlagWrapper
import no.nav.sokos.spk.mottak.domain.Avregningsretur
import no.nav.sokos.spk.mottak.domain.toAvregningsretur
import no.nav.sokos.spk.mottak.dto.Avregningstransaksjon
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.metrics.Metrics.mqAvregningListenerMetricCounter
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction
import no.nav.sokos.spk.mottak.util.Utils.toIsoDate
import no.nav.sokos.spk.mottak.util.Utils.toLocalDate

internal class AvregningServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, MQListener))

    val json = Json { ignoreUnknownKeys = true }

    val avregningService: AvregningService by lazy {
        AvregningService(
            connectionFactory,
            ActiveMQQueue(PropertiesConfig.MQProperties().avregningsgrunnlagQueueName),
            Db2Listener.dataSource,
        )
    }

    val jmsProducerAvregning: JmsProducerService by lazy {
        JmsProducerService(
            ActiveMQQueue(PropertiesConfig.MQProperties().avregningsgrunnlagQueueName),
            ActiveMQQueue(PropertiesConfig.MQProperties().avregningsgrunnlagQueueName),
            mqAvregningListenerMetricCounter,
            connectionFactory,
        )
    }

    listOf(
        TestScenario(
            description = "det sendes en avregningsmelding med delYtelseId til MQ",
            jsonFile = "/mq/avregning_med_kjent_utbetalingstransaksjon.json",
            expectedMetricValue = 1,
            motId = "20025925",
            fnr = "04030842389",
            transEksId = "9805367",
            datoAvsender = "2008-12-20",
            transaksjonId = 20025925,
        ),
        TestScenario(
            description = "det sendes en avregningsmelding med trekkvedtakId til MQ",
            jsonFile = "/mq/avregning_med_kjent_trekktransaksjon.json",
            expectedMetricValue = 2,
            trekkvedtakId = "123456",
            fnr = "19040835672",
            transEksId = "9805382",
            datoAvsender = "2008-12-20",
            transaksjonId = 20025935,
        ),
        TestScenario(
            description = "det sendes en avregningsmelding med kreditorRef til MQ",
            jsonFile = "/mq/avregning_med_ukjent_trekktransaksjon.json",
            expectedMetricValue = 3,
            trekkvedtakId = "223344",
            transEksId = "918273",
            datoAvsender = UNKNOWN_TRANSACTION_DATE,
        ),
        TestScenario(
            description = "det sendes en avregningsmelding med ukjent transaksjon til MQ",
            jsonFile = "/mq/avregning_ukjent_utbetalingstransaksjon.json",
            expectedMetricValue = 4,
            motId = "999888777",
            datoAvsender = UNKNOWN_TRANSACTION_DATE,
        ),
    ).forEach { scenario ->
        Given("det finnes avregningsmeldinger som skal sendes fra UR Z") {
            setupDatabase("/database/utbetaling_transaksjon.sql")

            When(scenario.description) {
                avregningService.start()
                val avregningsmelding = readFromResource(scenario.jsonFile)
                jmsProducerAvregning.send(listOf(avregningsmelding))

                Then("blir det mottatt en melding") {
                    mqAvregningListenerMetricCounter.longValue shouldBe scenario.expectedMetricValue
                    runBlocking {
                        delay(2000)
                        val avregningsgrunnlagWrapper = json.decodeFromString<AvregningsgrunnlagWrapper>(avregningsmelding)
                        val avregningsretur =
                            run {
                                scenario.trekkvedtakId?.let {
                                    Db2Listener.avregningsreturRepository.getByTrekkvedtakId(it)
                                } ?: scenario.motId?.let {
                                    Db2Listener.avregningsreturRepository.getByMotId(it)
                                } ?: avregningsgrunnlagWrapper.avregningsgrunnlag.toAvregningsretur(
                                    Avregningstransaksjon(
                                        datoAnviser = UNKNOWN_TRANSACTION_DATE.toIsoDate(),
                                    ),
                                )
                            }

                        verifyAvregningstransaksjon(
                            avregningsretur,
                            avregningsgrunnlagWrapper,
                            avregningFnr = scenario.fnr,
                            avregningTransEksId = scenario.transEksId,
                            avregningDatoAvsender = scenario.datoAvsender,
                            avregningTransaksjonId = scenario.transaksjonId,
                        )
                        run {
                            scenario.trekkvedtakId?.let {
                                Db2Listener.avregningsreturRepository.getByTrekkvedtakId(it)
                            } ?: scenario.motId?.let {
                                Db2Listener.avregningsreturRepository.getByMotId(it)
                            }
                        } shouldBe avregningsretur
                    }
                }
            }
        }
    }

    Given("det finnes avregningsmeldinger som skal sendes fra UR Z") {
        setupDatabase("/database/utbetaling_transaksjon.sql")
        setupDatabase("/database/avregningstransaksjon.sql")

        When("det sendes en avregningsmelding med delYtelseId til MQ som allerede eksisterer i databasen") {
            avregningService.start()
            Db2Listener.avregningsreturRepository.getByMotId("20025925").let { it?.osId shouldBe "999999" }
            val avregningsmelding = readFromResource("/mq/avregning_med_kjent_utbetalingstransaksjon.json")
            jmsProducerAvregning.send(listOf(avregningsmelding))

            Then("blir det mottatt en melding som er duplikat og som ikke blir lagret i databasen") {
                mqAvregningListenerMetricCounter.longValue shouldBe 5
                runBlocking {
                    delay(2000)
                    Db2Listener.avregningsreturRepository.getByMotId("20025925").let { it?.osId shouldBe "999999" }
                    Db2Listener.avregningsreturRepository.getNoOfRows() shouldBe 1
                }
            }
        }
    }

    Given("det finnes avregningsmeldinger som skal sendes fra UR Z") {
        setupDatabase("/database/utbetaling_transaksjon.sql")

        When("det sendes en avregningsmelding med delYtelseId til MQ som har formatsfeil") {
            avregningService.start()
            val avregningsmelding = readFromResource("/mq/avregning_med_formatsfeil.json")
            jmsProducerAvregning.send(listOf(avregningsmelding))

            Then("blir meldingen forkastet pga formatsfeil i 'tomdato'") {
                mqAvregningListenerMetricCounter.longValue shouldBe 6
                runBlocking {
                    delay(2000)
                    Db2Listener.avregningsreturRepository.getNoOfRows() shouldBe 0
                    Db2Listener.avregningsavvikRepository.getNoOfRows() shouldBe 1
                    Db2Listener.avregningsavvikRepository.getFeilmeldingByBilagsNr("10", "759197901") shouldBe "Feil ved konvertering av 20091313 (format yyyyMMdd) til dato"
                }
            }
        }
    }
})

private fun setupDatabase(dbScript: String) {
    Db2Listener.dataSource.transaction { session ->
        session.update(queryOf(readFromResource(dbScript)))
    }
}

private fun verifyAvregningstransaksjon(
    avregningsretur: Avregningsretur,
    avregningsmelding: AvregningsgrunnlagWrapper,
    avregningFnr: String? = null,
    avregningTransEksId: String? = null,
    avregningDatoAvsender: String? = null,
    avregningTransaksjonId: Int? = null,
) {
    val systemId = PropertiesConfig.Configuration().naisAppName
    val avregningsgrunnlag = avregningsmelding.avregningsgrunnlag
    with(avregningsretur) {
        osId shouldBe avregningsgrunnlag.oppdragsId.toString()
        osLinjeId shouldBe avregningsgrunnlag.linjeId?.toString()
        trekkvedtakId shouldBe avregningsgrunnlag.trekkvedtakId?.toString()
        gjelderId shouldBe avregningsgrunnlag.gjelderId
        fnr shouldBe avregningFnr
        datoStatus shouldBe avregningsgrunnlag.datoStatusSatt.toLocalDate()
        status shouldBe avregningsgrunnlag.status
        bilagsnrSerie shouldBe avregningsgrunnlag.bilagsnrSerie
        bilagsnr shouldBe avregningsgrunnlag.bilagsnr
        datoFom shouldBe avregningsgrunnlag.fomdato.toLocalDate()
        datoTom shouldBe avregningsgrunnlag.tomdato.toLocalDate()
        belop shouldBe avregningsgrunnlag.belop.toString()
        debetKredit shouldBe avregningsgrunnlag.debetKredit
        utbetalingtype shouldBe avregningsgrunnlag.utbetalingsType
        transTekst shouldBe avregningsgrunnlag.transTekst
        transEksId shouldBe avregningTransEksId
        datoAvsender shouldBe avregningDatoAvsender?.toIsoDate()
        utbetalesTil shouldBe avregningsgrunnlag.utbetalesTil
        transaksjonId shouldBe avregningTransaksjonId
        datoValutering shouldBe avregningsgrunnlag.datoValutert
        konto shouldBe avregningsgrunnlag.konto
        motId shouldBe avregningsgrunnlag.delytelseId
        personId shouldBe avregningsgrunnlag.fagSystemId
        kreditorRef shouldBe avregningsgrunnlag.kreditorRef
        datoOpprettet.toLocalDate() shouldBe LocalDate.now()
        opprettetAv shouldBe systemId
        datoEndret.toLocalDate() shouldBe LocalDate.now()
        endretAv shouldBe systemId
    }
}

data class TestScenario(
    val description: String,
    val jsonFile: String,
    val expectedMetricValue: Long,
    val motId: String? = null,
    val trekkvedtakId: String? = null,
    val fnr: String? = null,
    val transEksId: String? = null,
    val datoAvsender: String? = null,
    val transaksjonId: Int? = null,
)
