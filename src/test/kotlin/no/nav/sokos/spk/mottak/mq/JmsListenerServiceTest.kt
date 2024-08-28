package no.nav.sokos.spk.mottak.mq

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_OK
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
                Db2Listener.dataSource,
                ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
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

        Given("det fins utbetaling MQ meldinger til oppdragZ") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            }

            When("hent utbetalinger og send til OppdragZ") {
                jmsListenerService.start()

                val reply = readFromResource("/mq/utbetaling_ok_kvittering.xml")
                jmsProducer.send(listOf(reply))

                Then("skal det returnere en OK meldingen tilbake") {
                    mqUtbetalingProducerMetricCounter.longValue shouldBe 1
                    runBlocking {
                        delay(1000)
                        mqUtbetalingListenerMetricCounter.longValue shouldBe 2

                        val transaksjonIdList = listOf(20025925, 20025926)
                        val transaksjonList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonIdList)
                        transaksjonList.map {
                            it.transaksjonTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_RETUR_OK
                        }
                        val transTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonIdList)
                        transTilstandList.map {
                            it.transaksjonTilstandType == TRANS_TILSTAND_OPPDRAG_RETUR_OK
                        }
                    }
                }
            }
        }
    })
