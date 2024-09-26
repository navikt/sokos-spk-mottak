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
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
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

        val jmsProducer: JmsProducerService by lazy {
            JmsProducerService(
                ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                mqUtbetalingProducerMetricCounter,
                connectionFactory,
            )
        }

        Given("det finnes utbetalingsmeldinger som skal sendes til oppdragZ") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            }

            When("henter utbetalinger og sender til OppdragZ") {
                jmsListenerService.start()

                val reply = readFromResource("/mq/utbetaling_ok_kvittering.xml")
                jmsProducer.send(listOf(reply))

                Then("skal det returneres en OK-melding tilbake") {
                    mqUtbetalingProducerMetricCounter.longValue shouldBe 1
                    runBlocking {
                        delay(1000)
                        mqUtbetalingListenerMetricCounter.longValue shouldBe 2

                        val transaksjonIdList = listOf(20025925, 20025926)
                        transaksjonIdList.forEach {
                            val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(it)!!
                            transaksjon.transTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_RETUR_OK
                            transaksjon.osStatus shouldBe "00"
                        }

                        val transTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonIdList)
                        transTilstandList.map {
                            it.transaksjonTilstandType == TRANS_TILSTAND_OPPDRAG_RETUR_OK
                        }
                    }
                }
            }
        }

        Given("det finnes flere utbetalingsmeldinger som skal sendes til oppdragZ") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            }

            When("henter utbetalinger og sender til OppdragZ") {
                jmsListenerService.start()

                val reply = readFromResource("/mq/utbetaling_feil_kvittering.xml")
                jmsProducer.send(listOf(reply))

                Then("skal det returneres en feilmelding tilbake") {
                    mqUtbetalingProducerMetricCounter.longValue shouldBe 2
                    runBlocking {
                        delay(1000)
                        mqUtbetalingListenerMetricCounter.longValue shouldBe 3

                        val transaksjonIdList = listOf(20025934)
                        transaksjonIdList.size shouldBe 1
                        val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(20025934)!!
                        transaksjon.transTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
                        transaksjon.osStatus shouldBe "08"

                        val transTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonIdList)
                        transTilstandList.map {
                            it.transaksjonTilstandType == TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
                        }
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
                jmsProducer.send(listOf(reply))

                Then("skal det returneres en duplikatmelding tilbake") {
                    mqUtbetalingProducerMetricCounter.longValue shouldBe 3
                    runBlocking {
                        delay(1000)
                        mqUtbetalingListenerMetricCounter.longValue shouldBe 3

                        val transaksjonIdList = listOf(20025934)
                        transaksjonIdList.size shouldBe 1
                        val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(20025934)!!
                        transaksjon.transTilstandType shouldBe TRANS_TILSTAND_OPPRETTET
                        transaksjon.osStatus shouldBe "00"

                        val transTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonIdList)
                        transTilstandList.map {
                            it.transaksjonTilstandType == TRANS_TILSTAND_OPPRETTET
                        }
                    }
                }
            }
        }
    })
