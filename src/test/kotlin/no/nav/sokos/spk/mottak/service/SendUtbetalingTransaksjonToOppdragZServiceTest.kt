package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.TestHelper.toUtbetalingsOppdrag
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactory
import no.nav.sokos.spk.mottak.listener.MQListener.replyQueueMock
import no.nav.sokos.spk.mottak.listener.MQListener.senderQueueMock
import no.nav.sokos.spk.mottak.metrics.Metrics.mqUtbetalingProducerMetricCounter
import no.nav.sokos.spk.mottak.mq.JmsProducerService
import no.nav.sokos.spk.mottak.util.JaxbUtils
import org.apache.activemq.artemis.jms.client.ActiveMQQueue
import java.sql.SQLException

const val BATCH_SIZE = 2

internal class SendUtbetalingTransaksjonToOppdragZServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, MQListener))

    Given("det finnes utbetalinger som skal sendes til oppdragZ") {
        val utbetalingTransaksjonTilOppdragService: SendUtbetalingTransaksjonToOppdragZService by lazy {
            SendUtbetalingTransaksjonToOppdragZService(
                dataSource = Db2Listener.dataSource,
                producer =
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                        mqUtbetalingProducerMetricCounter,
                        connectionFactory,
                    ),
            )
        }

        setupDatabase("/database/utbetaling_transaksjon.sql")

        val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
        transaksjoner.size shouldBe 10

        When("hent utbetalinger og send til OppdragZ") {
            utbetalingTransaksjonTilOppdragService.getUtbetalingTransaksjonAndSendToOppdragZ()
            Then("skal alle transaksjoner blir oppdatert med status OSO (Oppdrag Sendt OK)") {
                verifyDatabaseState(TRANS_TILSTAND_OPPDRAG_SENDT_OK, 10)
            }
        }
    }

    Given("det finnes utbetalinger som skal sendes til oppdragZ med MQ server som er nede") {
        val utbetalingTransaksjonTilOppdragService =
            SendUtbetalingTransaksjonToOppdragZService(
                dataSource = Db2Listener.dataSource,
                producer =
                    JmsProducerService(
                        senderQueueMock,
                        replyQueueMock,
                        mqUtbetalingProducerMetricCounter,
                        connectionFactory,
                    ),
            )

        setupDatabase("/database/utbetaling_transaksjon.sql")

        val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
        transaksjoner.size shouldBe 10

        When("hent utbetalinger og send til OppdragZ") {
            utbetalingTransaksjonTilOppdragService.getUtbetalingTransaksjonAndSendToOppdragZ()
            Then("skal alle transaksjoner blir oppdatert med status OSF (Oppdrag Sendt Feil)") {
                verifyDatabaseState(TRANS_TILSTAND_OPPDRAG_SENDT_FEIL, 10)
            }
        }
    }

    Given("det finnes utbetalinger som skal sendes til oppdragZ med database som er nede") {
        val dataSourceMock = mockk<HikariDataSource>()
        every { dataSourceMock.connection } throws SQLException("No database connection!")

        val utbetalingTransaksjonTilOppdragService =
            SendUtbetalingTransaksjonToOppdragZService(
                dataSource = dataSourceMock,
                producer =
                    JmsProducerService(
                        senderQueueMock,
                        replyQueueMock,
                        mqUtbetalingProducerMetricCounter,
                        connectionFactory,
                    ),
            )

        When("hent utbetalinger og send til OppdragZ") {
            val exception = shouldThrow<MottakException> { utbetalingTransaksjonTilOppdragService.getUtbetalingTransaksjonAndSendToOppdragZ() }
            Then("skal det kaste en feilmelding og SendUtbetalingTransaksjonToOppdragServiceV2 stoppet") {
                exception.message shouldBe "Feil under sending av utbetalingstransaksjoner til OppdragZ. Feilmelding: No database connection!"
            }
        }
    }

    Given("det finnes utbetalinger som skal sendes til oppdragZ med database som feiler ved oppdatering av transtilstand i transaksjonstabellen") {
        val utbetalingTransaksjonTilOppdragService =
            SendUtbetalingTransaksjonToOppdragZService(
                dataSource = Db2Listener.dataSource,
                transaksjonRepository = Db2Listener.transaksjonRepository,
                transaksjonTilstandRepository = Db2Listener.transaksjonTilstandRepository,
                producer =
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                        mqUtbetalingProducerMetricCounter,
                        connectionFactory,
                    ),
            )

        setupDatabase("/database/utbetaling_transaksjon.sql")

        val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
        transaksjoner.size shouldBe 10

        When("hent utbetalinger og send til OppdragZ") {
            clearMocks(Db2Listener.transaksjonTilstandRepository)
            every {
                Db2Listener.transaksjonRepository.updateBatch(any(), any(), eq(TRANS_TILSTAND_OPPDRAG_SENDT_OK), any(), any(), any())
            } throws Exception("Feiler ved oppdatering av transtilstand til OSO i transaksjon-tabellen!")
            utbetalingTransaksjonTilOppdragService.getUtbetalingTransaksjonAndSendToOppdragZ()
            Then("skal ingen transaksjoner blir oppdatert med status OSO (Oppdrag Sendt Ok), men beholder status OPR (Opprettet)") {
                verifyDatabaseState(TRANS_TILSTAND_OPPRETTET)
            }
        }
    }

    Given("det finnes utbetalinger som skal sendes til oppdragZ med database som feiler ved opprettelse av transaksjoner i transaksjontilstandtabellen") {
        val utbetalingTransaksjonTilOppdragService =
            SendUtbetalingTransaksjonToOppdragZService(
                dataSource = Db2Listener.dataSource,
                transaksjonRepository = Db2Listener.transaksjonRepository,
                transaksjonTilstandRepository = Db2Listener.transaksjonTilstandRepository,
                producer =
                    JmsProducerService(
                        ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingQueueName),
                        ActiveMQQueue(PropertiesConfig.MQProperties().utbetalingReplyQueueName),
                        mqUtbetalingProducerMetricCounter,
                        connectionFactory,
                    ),
            )

        setupDatabase("/database/utbetaling_transaksjon.sql")

        val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
        transaksjoner.size shouldBe 10

        When("hent utbetalinger og send til OppdragZ") {
            clearMocks(Db2Listener.transaksjonRepository)
            every {
                Db2Listener.transaksjonTilstandRepository.insertBatch(any(), TRANS_TILSTAND_OPPDRAG_SENDT_OK, any(), any(), any())
            } throws Exception("Feiler ved opprettelse av transaksjoner i transaksjontilstand-tabellen!")
            utbetalingTransaksjonTilOppdragService.getUtbetalingTransaksjonAndSendToOppdragZ()
            Then("skal ingen transaksjoner blir oppdatert med status OSO (Oppdrag Sendt Ok), men beholder status OPR (Opprettet)") {
                verifyDatabaseState(TRANS_TILSTAND_OPPRETTET)
            }
        }
    }

    Given("det finnes utbetalinger som skal sendes til oppdragZ med MQ server nede og som feiler ved opprettelse av transaksjoner med feilstatus i transaksjontilstandtabellen") {
        val utbetalingTransaksjonTilOppdragService =
            SendUtbetalingTransaksjonToOppdragZService(
                dataSource = Db2Listener.dataSource,
                transaksjonRepository = Db2Listener.transaksjonRepository,
                transaksjonTilstandRepository = Db2Listener.transaksjonTilstandRepository,
                producer =
                    JmsProducerService(
                        senderQueueMock,
                        replyQueueMock,
                        mqUtbetalingProducerMetricCounter,
                        connectionFactory,
                    ),
            )

        setupDatabase("/database/utbetaling_transaksjon.sql")

        val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
        transaksjoner.size shouldBe 10

        When("hent utbetalinger og send til OppdragZ") {
            clearMocks(Db2Listener.transaksjonRepository)
            every {
                Db2Listener.transaksjonTilstandRepository.insertBatch(any(), TRANS_TILSTAND_OPPDRAG_SENDT_FEIL, any(), any(), any())
            } throws Exception("Feiler ved opprettelse av transaksjoner med feilstatus i transaksjontilstand-tabellen!")
            utbetalingTransaksjonTilOppdragService.getUtbetalingTransaksjonAndSendToOppdragZ()
            Then("skal ingen transaksjoner blir oppdatert med status OSF (Oppdrag Sendt Feil), men beholder status OPR (Opprettet)") {
                verifyDatabaseState(TRANS_TILSTAND_OPPRETTET)
            }
        }
    }

    Given("det finnes 5 utbetalinger som skal sendes til oppdragZ i en batch-chunk") {
        val producer = mockk<JmsProducerService>(relaxed = true)
        val utbetalingTransaksjonTilOppdragService: SendUtbetalingTransaksjonToOppdragZService by lazy {
            SendUtbetalingTransaksjonToOppdragZService(
                dataSource = Db2Listener.dataSource,
                producer = producer,
            )
        }

        setupDatabase("/database/utbetaling_person_flere_transaksjoner.sql")

        val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
        transaksjoner.size shouldBe 5

        When("hent utbetalinger og send til OppdragZ") {
            utbetalingTransaksjonTilOppdragService.getUtbetalingTransaksjonAndSendToOppdragZ(BATCH_SIZE)
            Then("skal alle utbetalinger blir sent i en batch-chunk") {
                val slot = slot<List<String>>()
                verify { producer.send(capture(slot)) }
                verify(exactly = 1) { producer.send(any()) }

                val sentMessages = slot.captured
                sentMessages.size shouldBe 2
                sentMessages.first().contains("<kodeEndring>NY</kodeEndring>") shouldBe true
                sentMessages.last().contains("<kodeEndring>UEND</kodeEndring>") shouldBe true

                val expectedMessages = transaksjoner.toExpectedMessages()
                sentMessages shouldContainExactly expectedMessages

                verifyDatabaseState(TRANS_TILSTAND_OPPDRAG_SENDT_OK, 5)
            }
        }
    }

    Given("det finnes utbetalinger for en person som skal sendes til oppdragZ") {
        val producer = mockk<JmsProducerService>(relaxed = true)
        val utbetalingTransaksjonTilOppdragService: SendUtbetalingTransaksjonToOppdragZService by lazy {
            SendUtbetalingTransaksjonToOppdragZService(
                dataSource = Db2Listener.dataSource,
                producer = producer,
            )
        }

        setupDatabase("/database/utbetaling_person_fnr_endret.sql")

        val transaksjoner = Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG)
        transaksjoner.size shouldBe 1

        When("hent utbetalinger og send til OppdragZ") {
            utbetalingTransaksjonTilOppdragService.getUtbetalingTransaksjonAndSendToOppdragZ()
            Then("skal en transaksjon bli oppdatert med status OSO (Oppdrag Sendt OK) og meldingen som sendes til OppdragZ skal ha status 'ENDR'") {
                val slot = slot<List<String>>()
                verify { producer.send(capture(slot)) }
                val sentMessage = slot.captured
                sentMessage.size shouldBe 1
                sentMessage.first().contains("<kodeEndring>ENDR</kodeEndring>") shouldBe true

                val expectedMessages = transaksjoner.toExpectedMessages()
                sentMessage shouldContainExactly expectedMessages

                verifyDatabaseState(TRANS_TILSTAND_OPPDRAG_SENDT_OK, 1)
            }
        }
    }
})

private fun setupDatabase(dbScript: String) {
    Db2Listener.dataSource.transaction { session ->
        session.update(queryOf(readFromResource(dbScript)))
    }
}

private fun verifyDatabaseState(
    status: String,
    noOfTransactions: Int,
) {
    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000002)
    transaksjonList.forEach { it.transTilstandType shouldBe status }

    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
    transaksjonTilstandList.size shouldBe noOfTransactions
    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe status }
}

private fun verifyDatabaseState(status: String) {
    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000002)
    transaksjonList.forEach { it.transTilstandType shouldBe status }

    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
    transaksjonTilstandList.size shouldBe 0
}

private fun List<Transaksjon>.toExpectedMessages(): List<String> {
    return this.groupBy { Pair(it.personId, it.gyldigKombinasjon!!.fagomrade) }
        .map { it.value.toUtbetalingsOppdrag() }
        .map { JaxbUtils.marshallOppdrag(it) }
}
