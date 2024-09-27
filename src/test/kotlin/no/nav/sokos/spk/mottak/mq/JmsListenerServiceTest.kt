package no.nav.sokos.spk.mottak.mq

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_RETUR_OK
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.metrics.Metrics.mqTrekkListenerMetricCounter
import no.nav.sokos.spk.mottak.metrics.Metrics.mqTrekkProducerMetricCounter
import no.nav.sokos.spk.mottak.metrics.Metrics.mqUtbetalingListenerMetricCounter
import no.nav.sokos.spk.mottak.metrics.Metrics.mqUtbetalingProducerMetricCounter
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

internal class JmsListenerServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, MQListener))

        val jmsListenerService: JmsListenerService by lazy {
            JmsListenerService(
                connectionFactory,
                ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                Db2Listener.dataSource,
            )
        }

        val jmsProducerUtbetaling: JmsProducerService by lazy {
            JmsProducerService(
                ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                mqUtbetalingProducerMetricCounter,
                connectionFactory,
            )
        }

        val jmsProducerTrekk: JmsProducerService by lazy {
            JmsProducerService(
                ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                mqTrekkProducerMetricCounter,
                connectionFactory,
            )
        }

        Given("det finnes utbetalingsmeldinger som skal sendes til oppdragZ") {

            setupDatabase("/database/utbetaling_transaksjon.sql")

            When("henter utbetalinger og sender til OppdragZ") {
                jmsListenerService.start()

                val reply = readFromResource("/mq/utbetaling_ok_kvittering.xml")
                jmsProducerUtbetaling.send(listOf(reply))

                Then("skal det returneres en OK-utbetalingsmelding tilbake") {
                    mqUtbetalingProducerMetricCounter.longValue shouldBe 1
                    runBlocking {
                        delay(2000)
                        mqUtbetalingListenerMetricCounter.longValue shouldBe 2
                        val transaksjonIdList = listOf(20025925, 20025926)
                        verifyTransaksjonState(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_RETUR_OK, "00")
                        verifyTransaksjonTilstandState(transaksjonIdList, TRANS_TILSTAND_OPPDRAG_RETUR_OK)
                    }
                }
            }
        }

        Given("det finnes flere utbetalingsmeldinger som skal sendes til oppdragZ") {

            setupDatabase("/database/utbetaling_transaksjon.sql")

            When("henter utbetalinger og sender til OppdragZ") {
                jmsListenerService.start()

                val reply = readFromResource("/mq/utbetaling_feil_kvittering.xml")
                jmsProducerUtbetaling.send(listOf(reply))

                Then("skal det returneres en utbetalingsfeilmelding tilbake") {
                    mqUtbetalingProducerMetricCounter.longValue shouldBe 2
                    runBlocking {
                        delay(1000)
                        mqUtbetalingListenerMetricCounter.longValue shouldBe 3
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
                jmsListenerService.start()

                val reply = readFromResource("/mq/utbetaling_feil_kvittering.xml")
                jmsProducerUtbetaling.send(listOf(reply))

                Then("skal det returneres en utbetalingsduplikatmelding tilbake") {
                    mqUtbetalingProducerMetricCounter.longValue shouldBe 3
                    runBlocking {
                        delay(1000)
                        mqUtbetalingListenerMetricCounter.longValue shouldBe 3
                        val transaksjonIdList = listOf(20025934)
                        verifyTransaksjonState(transaksjonIdList, TRANS_TILSTAND_OPPRETTET, "00")
                        verifyTransaksjonTilstandState(transaksjonIdList, TRANS_TILSTAND_OPPRETTET)
                    }
                }
            }
        }

        Given("det finnes trekkmeldinger som skal sendes til oppdragZ") {

            setupDatabase("/database/trekk_transaksjon.sql")

            When("henter trekkmeldinger og sender til OppdragZ") {
                jmsListenerService.start()

                val reply = readFromResource("/mq/trekk_ok_kvittering.xml")
                jmsProducerTrekk.send(listOf(reply))

                Then("skal det returneres en OK-trekkmelding tilbake") {
                    mqTrekkProducerMetricCounter.longValue shouldBe 1
                    runBlocking {
                        delay(1000)
                        mqTrekkListenerMetricCounter.longValue shouldBe 1
                        val transaksjonIdList = listOf(20425974)
                        verifyTransaksjonState(transaksjonIdList, TRANS_TILSTAND_TREKK_RETUR_OK, "00")
                        verifyTransaksjonTilstandState(transaksjonIdList, TRANS_TILSTAND_TREKK_RETUR_OK)
                    }
                }
            }
        }

        Given("det finnes flere trekkmeldinger som skal sendes til oppdragZ") {

            setupDatabase("/database/trekk_transaksjon.sql")

            When("henter trekkmeldinger og sender til OppdragZ") {
                jmsListenerService.start()

                val reply = readFromResource("/mq/trekk_feil_kvittering.xml")
                jmsProducerTrekk.send(listOf(reply))

                Then("skal det returneres en trekk-feilmelding tilbake") {
                    mqTrekkProducerMetricCounter.longValue shouldBe 2
                    runBlocking {
                        delay(1000)
                        mqTrekkListenerMetricCounter.longValue shouldBe 2
                        val transaksjonIdList = listOf(20425974)
                        verifyTransaksjonState(transaksjonIdList, TRANS_TILSTAND_TREKK_RETUR_FEIL, "08")
                        verifyTransaksjonTilstandState(transaksjonIdList, TRANS_TILSTAND_TREKK_RETUR_FEIL)
                    }
                }
            }
        }

        Given("det finnes enda flere trekkmeldinger som skal sendes til oppdragZ") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/trekk_transaksjon.sql")))
                session.update(queryOf("UPDATE T_TRANSAKSJON SET OS_STATUS = '00' WHERE TRANSAKSJON_ID = 20425974"))
            }

            When("henter trekkmeldinger og sender til OppdragZ") {
                jmsListenerService.start()

                val reply = readFromResource("/mq/trekk_feil_kvittering.xml")
                jmsProducerTrekk.send(listOf(reply))

                Then("skal det returneres en trekk-duplikatmelding tilbake") {
                    mqTrekkProducerMetricCounter.longValue shouldBe 3
                    runBlocking {
                        delay(1000)
                        mqTrekkListenerMetricCounter.longValue shouldBe 2
                        val transaksjonIdList = listOf(20425974)
                        verifyTransaksjonState(transaksjonIdList, TRANS_TILSTAND_OPPRETTET, "00")
                        verifyTransaksjonTilstandState(transaksjonIdList, TRANS_TILSTAND_OPPRETTET)
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

private fun verifyTransaksjonState(
    transaksjonIdList: List<Int>,
    transaksjonStatus: String,
    osStatus: String,
) {
    transaksjonIdList.forEach {
        val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(it)!!
        transaksjon.transTilstandType shouldBe transaksjonStatus
        transaksjon.osStatus shouldBe osStatus
    }
}

private fun verifyTransaksjonTilstandState(
    transaksjonIdList: List<Int>,
    transaksjonTilstandStatus: String,
) {
    val transTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonIdList)
    transTilstandList.map {
        it.transaksjonTilstandType == transaksjonTilstandStatus
    }
}
