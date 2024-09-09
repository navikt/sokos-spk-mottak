package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
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
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

internal class SendTrekkTransaksjonServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, MQListener))

        Given("det finnes trekk som skal sendes til oppdragZ") {
            val trekkTransaksjonTilOppdragService: SendTrekkTransaksjonTilOppdragService by lazy {
                SendTrekkTransaksjonTilOppdragService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
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
            When("henter trekk og sender til OppdragZ") {
                trekkTransaksjonTilOppdragService.hentTrekkTransaksjonOgSendTilOppdrag()
                Then("skal alle transaksjoner bli oppdatert med status TSO (Trekk Sendt OK)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_OK }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_OK }
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med MQ server nede") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonTilOppdragService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
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
            When("henter trekk og sender til OppdragZ") {
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

        Given("det finnes trekk som skal sendes til oppdragZ med database nede") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonTilOppdragService(
                    mockk<HikariDataSource>(),
                    mockk<TransaksjonRepository>(),
                    mockk<TransaksjonTilstandRepository>(),
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )
            When("henter trekk og sender til OppdragZ") {
                val exception = shouldThrow<RuntimeException> { trekkTransaksjonTilOppdragService.hentTrekkTransaksjonOgSendTilOppdrag() }

                Then("skal det kastes en feilmelding og SendTrekkTransaksjonTilOppdragService skal stoppes") {
                    exception.message shouldContain "Fatal feil ved henting av trekktransaksjoner"
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med database som feiler ved oppdatering av transtilstand i transaksjonstabellen") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonTilOppdragService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(TestHelper.readFromResource("/database/trekk_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK).size shouldBe 10
            When("henter trekk og sender til OppdragZ") {
                clearMocks(Db2Listener.transaksjonTilstandRepository)
                every {
                    Db2Listener.transaksjonRepository.updateBatch(any(), any(), eq(TRANS_TILSTAND_TREKK_SENDT_OK), any(), any(), any())
                } throws Exception("Feiler ved oppdatering av transtilstand til TSO i transaksjon-tabellen!")
                trekkTransaksjonTilOppdragService.hentTrekkTransaksjonOgSendTilOppdrag()
                Then("skal ingen transaksjoner blir oppdatert med status TSO (Trekk Sendt Ok), men bli oppdatert med status TSF (Trekk Sendt Feil)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_FEIL }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_FEIL }
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med database som feiler ved opprettelse av transaksjoner i transaksjontilstandtabellen") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonTilOppdragService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(TestHelper.readFromResource("/database/trekk_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK).size shouldBe 10
            When("henter trekk og sender til OppdragZ") {
                clearMocks(Db2Listener.transaksjonRepository)
                every {
                    Db2Listener.transaksjonTilstandRepository.insertBatch(any(), TRANS_TILSTAND_TREKK_SENDT_OK, any(), any(), any())
                } throws Exception("Feiler ved opprettelse av transaksjoner i transaksjontilstand-tabellen!")
                trekkTransaksjonTilOppdragService.hentTrekkTransaksjonOgSendTilOppdrag()
                Then("skal ingen transaksjoner blir oppdatert med status TSO (Trekk Sendt Ok), men bli oppdatert med status TSF (Trekk Sendt Feil)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_FEIL }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_FEIL }
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med database som feiler ved oppdatering av transtilstand i transaksjon-tabellen også etter at sendingen feilet") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonTilOppdragService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(TestHelper.readFromResource("/database/trekk_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK).size shouldBe 10
            When("henter trekk og sender til OppdragZ") {
                clearMocks(Db2Listener.transaksjonTilstandRepository)
                clearMocks(Db2Listener.transaksjonRepository)
                every {
                    Db2Listener.transaksjonRepository.updateBatch(any(), any(), any(), any(), any(), any())
                } throws Exception("Feiler ved oppdatering av transtilstand til TSF i transaksjon-tabellen!")

                val exception = shouldThrow<RuntimeException> { trekkTransaksjonTilOppdragService.hentTrekkTransaksjonOgSendTilOppdrag() }
                val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
                transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_OPPRETTET }
                val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                transaksjonTilstandList.size shouldBe 0

                Then("skal det kastes en fatal feil og SendTrekkTransaksjonTilOppdragService skal stoppes") {
                    exception.message shouldContain "Fatal feil ved sending av trekktransaksjoner"
                }
            }
        }
    })
