package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.listener.MQListener.replyQueueMock
import no.nav.sokos.spk.mottak.listener.MQListener.senderQueueMock
import no.nav.sokos.spk.mottak.metrics.Metrics.mqTrekkProducerMetricCounter
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

internal class SendTrekkTransaksjonServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, MQListener))

        Given("det finnes trekk som skal sendes til oppdragZ") {
            val trekkTransaksjonTilOppdragService: SendTrekkTransaksjonTilOppdragService by lazy {
                SendTrekkTransaksjonTilOppdragService(
                    Db2Listener.dataSource,
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )
            }

            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(TestHelper.readFromResource("/database/trekk_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK).size shouldBe 10
            When("hent trekk og send til OppdragZ") {
                trekkTransaksjonTilOppdragService.hentTrekkTransaksjonOgSendTilOppdrag()
                Then("skal alle transaksjoner blir oppdatert med status TSO (Trekk Sendt OK)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_OK }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_OK }
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med MQ server er nede") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonTilOppdragService(
                    Db2Listener.dataSource,
                    JmsProducerService(
                        senderQueueMock,
                        replyQueueMock,
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )

            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(TestHelper.readFromResource("/database/trekk_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK).size shouldBe 10
            When("hent trekk og send til OppdragZ") {
                trekkTransaksjonTilOppdragService.hentTrekkTransaksjonOgSendTilOppdrag()
                Then("skal alle transaksjoner blir oppdatert med status TSF (Trekk Sendt Feil)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_FEIL }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_FEIL }
                }
            }
        }
    })
