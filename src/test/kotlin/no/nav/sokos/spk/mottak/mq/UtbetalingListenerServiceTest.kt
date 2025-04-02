package no.nav.sokos.spk.mottak.mq

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.TestHelper.verifyTransaksjonState
import no.nav.sokos.spk.mottak.TestHelper.verifyTransaksjonTilstandState
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

class UtbetalingListenerServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, MQListener))

    val utbetalingListenerService: UtbetalingListenerService by lazy {
        UtbetalingListenerService(
            connectionFactory,
            ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
            producer =
                JmsProducerService(
                    senderQueue = ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName + "_BOQ"),
                    metricCounter = Metrics.mqUtbetalingBOQListenerMetricCounter,
                    connectionFactory = connectionFactory,
                ),
            Db2Listener.dataSource,
        )
    }

    val jmsProducerUtbetaling: JmsProducerService by lazy {
        JmsProducerService(
            ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
            ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
            Metrics.mqUtbetalingProducerMetricCounter,
            connectionFactory,
        )
    }

    Given("det finnes utbetalingsmeldinger som skal sendes til oppdragZ") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
        }

        When("henter utbetalinger og sender til OppdragZ") {
            utbetalingListenerService.start()

            val reply = readFromResource("/mq/utbetaling_ok_kvittering.xml")
            jmsProducerUtbetaling.send(listOf(reply))

            Then("skal det returneres en OK-utbetalingsmelding tilbake") {
                Metrics.mqUtbetalingProducerMetricCounter.longValue shouldBe 1
                runBlocking {
                    delay(2000)
                    Metrics.mqUtbetalingListenerMetricCounter.longValue shouldBe 2
                    val transaksjonIdList = listOf(20025925, 20025926)
                    verifyTransaksjonState(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_RETUR_OK, "00")
                    verifyTransaksjonTilstandState(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_RETUR_OK)
                }
            }
        }
    }

    Given("det finnes flere utbetalingsmeldinger som skal sendes til oppdragZ") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
        }

        When("henter utbetalinger og sender til OppdragZ") {
            utbetalingListenerService.start()

            val reply = readFromResource("/mq/utbetaling_feil_kvittering.xml")
            jmsProducerUtbetaling.send(listOf(reply))

            Then("skal det returneres en utbetalingsfeilmelding tilbake") {
                Metrics.mqUtbetalingProducerMetricCounter.longValue shouldBe 2
                runBlocking {
                    delay(1000)
                    Metrics.mqUtbetalingListenerMetricCounter.longValue shouldBe 3
                    val transaksjonIdList = listOf(20025934)
                    verifyTransaksjonState(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_RETUR_FEIL, "08")
                    verifyTransaksjonTilstandState(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_RETUR_FEIL)
                }
            }
        }
    }

    Given("det finnes enda flere utbetalingsmeldinger som skal sendes til oppdragZ") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            session.update(queryOf("UPDATE T_TRANSAKSJON SET OS_STATUS = '00' WHERE TRANSAKSJON_ID = 20025934"))
        }

        When("henter utbetalinger og sender til OppdragZ") {
            utbetalingListenerService.start()

            val reply = readFromResource("/mq/utbetaling_feil_kvittering.xml")
            jmsProducerUtbetaling.send(listOf(reply))

            Then("skal det returneres en utbetalingsduplikatmelding tilbake") {
                Metrics.mqUtbetalingProducerMetricCounter.longValue shouldBe 3
                runBlocking {
                    delay(1000)
                    Metrics.mqUtbetalingListenerMetricCounter.longValue shouldBe 3
                    val transaksjonIdList = listOf(20025934)
                    verifyTransaksjonState(transaksjonIdList, TRANS_TILSTAND_OPPRETTET, "00")
                    verifyTransaksjonTilstandState(transaksjonIdList, TRANS_TILSTAND_OPPRETTET)
                }
            }
        }
    }
})
