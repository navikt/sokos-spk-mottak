import java.time.LocalDate

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.avregning.Avregningsretur
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.metrics.Metrics.mqAvregningListenerMetricCounter
import no.nav.sokos.spk.mottak.mq.AvregningService
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

internal class AvregningServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, MQListener))

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
            jsonFile = "/mq/avregning_med_delytelseId.json",
            expectedMetricValue = 1,
            transaksjonId = 20025925,
        ),
        TestScenario(
            description = "det sendes en avregningsmelding med trekkvedtakId til MQ",
            jsonFile = "/mq/avregning_med_trekkvedtakId.json",
            expectedMetricValue = 2,
            transaksjonId = 20025935,
        ),
        TestScenario(
            description = "det sendes en avregningsmelding med kreditorRef til MQ",
            jsonFile = "/mq/avregning_med_kreditorRef.json",
            expectedMetricValue = 3,
            trekkvedtakId = "223344",
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
                        val avregningsretur =
                            scenario.transaksjonId?.let {
                                Db2Listener.avregningsreturRepository.getByTransaksjonId(it)
                            } ?: Db2Listener.avregningsreturRepository.getByTrekkvedtakId(scenario.trekkvedtakId!!)
                        verifyAvregningstransaksjon(avregningsretur!!)
                    }
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

private fun verifyAvregningstransaksjon(avregningsretur: Avregningsretur) {
    val systemId = PropertiesConfig.Configuration().naisAppName
    with(avregningsretur) {
        osId shouldBe osId
        osLinjeId shouldBe osLinjeId
        trekkvedtakId shouldBe trekkvedtakId
        gjelderId shouldBe gjelderId
        fnr shouldBe fnr
        datoStatus shouldBe datoStatus
        status shouldBe status
        bilagsNrSerie shouldBe bilagsNrSerie
        bilagsNr shouldBe bilagsNr
        datoFom shouldBe datoFom
        datoTom shouldBe datoTom
        belop shouldBe belop
        debetKredit shouldBe debetKredit
        utbetalingType shouldBe utbetalingType
        transaksjonTekst shouldBe transaksjonTekst
        transEksId shouldBe transEksId
        datoAvsender shouldBe datoAvsender
        utbetalesTil shouldBe utbetalesTil
        transaksjonId shouldBe transaksjonId
        datoValutering shouldBe datoValutering
        konto shouldBe konto
        motId shouldBe motId
        personId shouldBe personId
        kreditorRef shouldBe kreditorRef
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
    val transaksjonId: Int? = null,
    val trekkvedtakId: String? = null,
)
