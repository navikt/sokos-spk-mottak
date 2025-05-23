package no.nav.sokos.spk.mottak.service

import java.sql.SQLException

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import io.mockk.clearAllMocks
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotliquery.queryOf
import org.apache.activemq.artemis.jms.client.ActiveMQQueue

import no.nav.sokos.spk.mottak.TestHelper
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.domain.converter.TrekkConverter.innrapporteringTrekk
import no.nav.sokos.spk.mottak.exception.DatabaseException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.listener.MQListener.replyQueueMock
import no.nav.sokos.spk.mottak.listener.MQListener.senderQueueMock
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

const val TREKK_BATCH_SIZE = 2

internal class SendTrekkServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, MQListener))

        Given("det finnes trekk som skal sendes til oppdragZ") {
            val producer = mockk<JmsProducerService>(relaxed = true)
            val trekkTransaksjonTilOppdragService: SendTrekkService by lazy {
                SendTrekkService(
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
                SendTrekkService(
                    dataSource = Db2Listener.dataSource,
                    producer =
                        JmsProducerService(
                            senderQueueMock,
                            replyQueueMock,
                            Metrics.mqTrekkProducerMetricCounter,
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
            every { Db2Listener.innTransaksjonRepository.countByInnTransaksjon() }.returns(0)
            every { Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK) } throws SQLException("No database connection!")

            val trekkTransaksjonTilOppdragService =
                SendTrekkService(
                    dataSource = Db2Listener.dataSource,
                    innTransaksjonRepository = Db2Listener.innTransaksjonRepository,
                    transaksjonRepository = Db2Listener.transaksjonRepository,
                    producer =
                        JmsProducerService(
                            ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                            ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                            Metrics.mqTrekkProducerMetricCounter,
                            connectionFactory,
                        ),
                )

            When("henter trekk og sender til OppdragZ") {
                val exception = shouldThrow<DatabaseException> { trekkTransaksjonTilOppdragService.getTrekkTransaksjonAndSendToOppdrag() }
                clearAllMocks()
                Then("skal det kastes en feilmelding og SendTrekkTransaksjonToOppdragServiceV2 skal stoppes") {
                    exception.message shouldStartWith "Db2-feil: "
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med database som feiler ved oppdatering av transtilstand i transaksjonstabellen") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    producer =
                        JmsProducerService(
                            ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                            ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                            Metrics.mqTrekkProducerMetricCounter,
                            connectionFactory,
                        ),
                )

            setupDatabase("/database/trekk_transaksjon.sql")

            val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
            transaksjoner.size shouldBe 10

            When("henter trekk og sender til OppdragZ") {
                clearMocks(Db2Listener.transaksjonTilstandRepository)
                every {
                    Db2Listener.transaksjonRepository.updateBatch(any(), any(), eq(TRANS_TILSTAND_TREKK_SENDT_OK), any(), any(), any(), any())
                } throws Exception("Feiler ved oppdatering av transtilstand til TSO i transaksjon-tabellen!")
                trekkTransaksjonTilOppdragService.getTrekkTransaksjonAndSendToOppdrag()
                Then("skal ingen transaksjoner blir oppdatert med status TSO (Trekk Sendt Ok), men beholder status OPR (Opprettet)") {
                    verifyDatabaseState(TRANS_TILSTAND_OPPRETTET)
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med database som feiler ved opprettelse av transaksjoner i transaksjontilstandtabellen") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    producer =
                        JmsProducerService(
                            ActiveMQQueue(PropertiesConfig.MQProperties().trekkQueueName),
                            ActiveMQQueue(PropertiesConfig.MQProperties().trekkReplyQueueName),
                            Metrics.mqTrekkProducerMetricCounter,
                            connectionFactory,
                        ),
                )

            setupDatabase("/database/trekk_transaksjon.sql")

            val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
            transaksjoner.size shouldBe 10

            When("henter trekk og sender til OppdragZ") {
                clearMocks(Db2Listener.transaksjonRepository)
                every {
                    Db2Listener.transaksjonTilstandRepository.insertBatch(any(), TRANS_TILSTAND_TREKK_SENDT_OK, any(), any(), any(), any())
                } throws Exception("Feiler ved opprettelse av transaksjoner i transaksjontilstand-tabellen!")
                trekkTransaksjonTilOppdragService.getTrekkTransaksjonAndSendToOppdrag()
                Then("skal ingen transaksjoner blir oppdatert med status TSO (Trekk Sendt Ok), men beholder status OPR (Opprettet)") {
                    verifyDatabaseState(TRANS_TILSTAND_OPPRETTET)
                }
            }
        }

        Given("det finnes trekk som skal sendes til oppdragZ med MQ server nede og som feiler ved opprettelse av transaksjoner med feilstatus i transaksjontilstandtabellen") {
            val trekkTransaksjonTilOppdragService =
                SendTrekkService(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    producer =
                        JmsProducerService(
                            senderQueueMock,
                            replyQueueMock,
                            Metrics.mqTrekkProducerMetricCounter,
                            connectionFactory,
                        ),
                )

            setupDatabase("/database/trekk_transaksjon.sql")

            val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK)
            transaksjoner.size shouldBe 10

            When("henter trekk og sender til OppdragZ") {
                clearMocks(Db2Listener.transaksjonRepository)
                every {
                    Db2Listener.transaksjonTilstandRepository.insertBatch(any(), TRANS_TILSTAND_TREKK_SENDT_FEIL, any(), any(), any(), any())
                } throws DatabaseException("Feiler ved opprettelse av transaksjoner med feilstatus i transaksjontilstand-tabellen!")

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
    numberOfTransactions: Int,
) {
    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
    transaksjonList.forEach { it.transTilstandType shouldBe status }

    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
    transaksjonTilstandList.size shouldBe numberOfTransactions
    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe status }
}

private fun verifyDatabaseState(status: String) {
    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
    transaksjonList.forEach { it.transTilstandType shouldBe status }

    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
    transaksjonTilstandList.size shouldBe 0
}
