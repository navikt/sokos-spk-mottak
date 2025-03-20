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
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_RETUR_OK
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.metrics.Metrics.mqTrekkListenerMetricCounter
import no.nav.sokos.spk.mottak.metrics.Metrics.mqTrekkProducerMetricCounter
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

class TrekkListenerServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, MQListener))

    val trekkListenerService: TrekkListenerService by lazy {
        TrekkListenerService(
            connectionFactory,
            ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
            Db2Listener.dataSource,
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

    Given("det finnes trekkmeldinger som skal sendes til oppdragZ") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/trekk_transaksjon.sql")))
        }

        When("henter trekkmeldinger og sender til OppdragZ") {
            trekkListenerService.start()

            val reply = readFromResource("/mq/trekk_ok_kvittering.json")
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
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/trekk_transaksjon.sql")))
        }

        When("henter trekkmeldinger og sender til OppdragZ") {
            trekkListenerService.start()

            val reply = readFromResource("/mq/trekk_feil_kvittering.json")
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
            trekkListenerService.start()

            val reply = readFromResource("/mq/trekk_feil_kvittering.json")
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
