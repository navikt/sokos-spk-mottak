package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.domain.converter.TrekkConverter.innrapporteringTrekk
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.listener.MQListener.replyQueueMock
import no.nav.sokos.spk.mottak.listener.MQListener.senderQueueMock
import no.nav.sokos.spk.mottak.metrics.Metrics.mqTrekkProducerMetricCounter
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

const val TREKK_BATCH_SIZE = 2

internal class SendTrekkTransaksjonToOppdragZServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, MQListener))

        Given("det finnes trekk som skal sendes til oppdragZ") {
            val producer = mockk<JmsProducerService>(relaxed = true)
            val trekkTransaksjonTilOppdragService: SendTrekkTransaksjonToOppdragZService by lazy {
                SendTrekkTransaksjonToOppdragZService(
                    dataSource = Db2Listener.dataSource,
                    mqBatchSize = TREKK_BATCH_SIZE,
                    producer = producer,
                )
            }

            setupDatabase("/database/trekk_transaksjon.sql")

            val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
            transaksjoner.size shouldBe 10

            val sentMessages = mutableListOf<List<String>>()

            When("henter trekk og sender til OppdragZ") {
                trekkTransaksjonTilOppdragService.getTrekkTransaksjonAndSendToOppdrag()
                Then("skal alle transaksjoner bli oppdatert med status TSO (Trekk Sendt OK)") {
                    verifyDatabaseState(TRANS_TILSTAND_TREKK_SENDT_OK, 10)
                    verify(exactly = 5) { producer.send(capture(sentMessages)) }
                    sentMessages.size shouldBe 5

                    val sent = sentMessages.flatten()
                    val expectedMessages = transaksjoner.map { it.innrapporteringTrekk() }

                    for (msg in expectedMessages) {
                        sent shouldContain msg
                    }
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med MQ server nede") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonToOppdragZService(
                    dataSource = Db2Listener.dataSource,
                    producer =
                    JmsProducerService(
                        senderQueueMock,
                        replyQueueMock,
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )

            setupDatabase("/database/trekk_transaksjon.sql")

            val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
            transaksjoner.size shouldBe 10

            When("henter trekk og sender til OppdragZ") {
                trekkTransaksjonTilOppdragService.getTrekkTransaksjonAndSendToOppdrag()
                Then("skal alle transaksjoner blir oppdatert med status TSF (Trekk Sendt Feil)") {
                    verifyDatabaseState(TRANS_TILSTAND_TREKK_SENDT_FEIL, 10)
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med database nede") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonToOppdragZService(
                    dataSource = mockk<HikariDataSource>(),
                    producer =
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )

            When("henter trekk og sender til OppdragZ") {
                val exception = shouldThrow<RuntimeException> { trekkTransaksjonTilOppdragService.getTrekkTransaksjonAndSendToOppdrag() }

                Then("skal det kastes en feilmelding og SendTrekkTransaksjonToOppdragServiceV2 skal stoppes") {
                    exception.message shouldContain "Fatal feil ved henting av trekktransaksjoner"
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med database som feiler ved oppdatering av transtilstand i transaksjonstabellen") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonToOppdragZService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    producer =
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )

            setupDatabase("/database/trekk_transaksjon.sql")

            val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
            transaksjoner.size shouldBe 10

            When("henter trekk og sender til OppdragZ") {
                clearMocks(Db2Listener.transaksjonTilstandRepository)
                every {
                    Db2Listener.transaksjonRepository.updateBatch(any(), any(), eq(TRANS_TILSTAND_TREKK_SENDT_OK), any(), any(), any())
                } throws Exception("Feiler ved oppdatering av transtilstand til TSO i transaksjon-tabellen!")
                trekkTransaksjonTilOppdragService.getTrekkTransaksjonAndSendToOppdrag()
                Then("skal ingen transaksjoner blir oppdatert med status TSO (Trekk Sendt Ok), men beholder status OPR (Opprettet)") {
                    verifyDatabaseState(TRANS_TILSTAND_OPPRETTET)
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med database som feiler ved opprettelse av transaksjoner i transaksjontilstandtabellen") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonToOppdragZService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    producer =
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )

            setupDatabase("/database/trekk_transaksjon.sql")

            val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
            transaksjoner.size shouldBe 10

            When("henter trekk og sender til OppdragZ") {
                clearMocks(Db2Listener.transaksjonRepository)
                every {
                    Db2Listener.transaksjonTilstandRepository.insertBatch(any(), TRANS_TILSTAND_TREKK_SENDT_OK, any(), any(), any())
                } throws Exception("Feiler ved opprettelse av transaksjoner i transaksjontilstand-tabellen!")
                trekkTransaksjonTilOppdragService.getTrekkTransaksjonAndSendToOppdrag()
                Then("skal ingen transaksjoner blir oppdatert med status TSO (Trekk Sendt Ok), men beholder status OPR (Opprettet)") {
                    verifyDatabaseState(TRANS_TILSTAND_OPPRETTET)
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med MQ server nede og som feiler ved opprettelse av transaksjoner med feilstatus i transaksjontilstandtabellen") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkTransaksjonToOppdragZService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    producer =
                    JmsProducerService(
                        senderQueueMock,
                        replyQueueMock,
                        mqTrekkProducerMetricCounter,
                        connectionFactory,
                    ),
                )

            setupDatabase("/database/trekk_transaksjon.sql")

            val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
            transaksjoner.size shouldBe 10

            When("henter trekk og sender til OppdragZ") {
                clearMocks(Db2Listener.transaksjonRepository)
                every {
                    Db2Listener.transaksjonTilstandRepository.insertBatch(any(), TRANS_TILSTAND_TREKK_SENDT_FEIL, any(), any(), any())
                } throws Exception("Feiler ved opprettelse av transaksjoner med feilstatus i transaksjontilstand-tabellen!")
                trekkTransaksjonTilOppdragService.getTrekkTransaksjonAndSendToOppdrag()
                Then("skal ingen transaksjoner blir oppdatert med status TSF (Trekk Sendt Feil), men beholder status OPR (Opprettet)") {
                    verifyDatabaseState(TRANS_TILSTAND_OPPRETTET)
                }
            }
        }
    })

private fun setupDatabase(dbScript: String) {
    Db2Listener.dataSource.transaction { session ->
        session.update(queryOf(TestHelper.readFromResource(dbScript)))
    }
}

private fun verifyDatabaseState(
    status: String,
    noOfTransactions: Int,
) {
    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
    transaksjonList.forEach { it.transTilstandType shouldBe status }

    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
    transaksjonTilstandList.size shouldBe noOfTransactions
    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe status }
}

private fun verifyDatabaseState(status: String) {
    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
    transaksjonList.forEach { it.transTilstandType shouldBe status }

    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
    transaksjonTilstandList.size shouldBe 0
}
