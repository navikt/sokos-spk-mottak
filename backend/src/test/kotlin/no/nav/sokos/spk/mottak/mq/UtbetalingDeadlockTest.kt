package no.nav.sokos.spk.mottak.mq

import java.sql.SQLException
import java.util.UUID

import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifyOrder
import jakarta.jms.ConnectionFactory
import jakarta.jms.JMSConsumer
import jakarta.jms.JMSContext
import jakarta.jms.Message
import jakarta.jms.MessageListener
import jakarta.jms.Queue
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.DatabaseTestConfig
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_OK
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

internal class UtbetalingDeadlockTest :
    FunSpec({
        lateinit var dataSource: HikariDataSource
        lateinit var repository: TransaksjonRepository
        lateinit var historyRepository: TransaksjonTilstandRepository
        lateinit var service: UtbetalingListenerService
        lateinit var message: Message
        lateinit var producer: JmsProducerService
        val callback = slot<MessageListener>()
        val payload = readFromResource("/mq/utbetaling_ok_kvittering.xml")

        beforeTest {
            dataSource =
                HikariDataSource(
                    DatabaseTestConfig.hikariConfig().apply {
                        jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};MODE=DB2"
                    },
                )
            dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/db2Script.sql")))
                session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            }
            repository = spyk(TransaksjonRepository(dataSource))
            historyRepository = TransaksjonTilstandRepository(dataSource)
            val context = mockk<JMSContext>(relaxed = true)
            val factory = mockk<ConnectionFactory>()
            val queue = mockk<Queue>()
            val consumer = mockk<JMSConsumer>()
            every { factory.createContext(JMSContext.CLIENT_ACKNOWLEDGE) } returns context
            every { context.createConsumer(queue) } returns consumer
            justRun { consumer.setMessageListener(capture(callback)) }
            producer = mockk(relaxed = true)
            message = mockk(relaxed = true)
            every { message.getBody(String::class.java) } returns payload
            service =
                UtbetalingListenerService(
                    factory,
                    queue,
                    producer,
                    dataSource,
                    repository,
                    historyRepository,
                )
        }

        afterTest {
            service.close()
            dataSource.close()
        }

        test("recovers from deadlocks before acknowledging without duplicate history or BOQ delivery") {
            var attempts = 0
            val initialCount = Metrics.mqUtbetalingListenerMetricCounter.longValue
            every { repository.updateBatch(any(), any(), any(), any(), any(), any(), any()) } answers {
                attempts++
                verify(exactly = 0) { message.acknowledge() }
                if (attempts < 3) throw SQLException("SQLERRMC=00C90088;00000302", "57033", -913)
                callOriginal()
            }
            every { message.acknowledge() } answers {
                repository.getByTransaksjonId(20025925)!!.transTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_RETUR_OK
                repository.getByTransaksjonId(20025926)!!.transTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_RETUR_OK
            }

            callback.captured.onMessage(message)

            attempts shouldBe 3
            Metrics.mqUtbetalingListenerMetricCounter.longValue shouldBe initialCount + 2
            historyRepository.findAllByTransaksjonId(listOf(20025925, 20025926)).count {
                it.transaksjonTilstandType == TRANS_TILSTAND_OPPDRAG_RETUR_OK
            } shouldBe 2
            verify(exactly = 1) { message.acknowledge() }
            verify(exactly = 0) { producer.send(any()) }
        }

        test("sends to BOQ once after all deadlock retries are exhausted") {
            val initialCount = Metrics.mqUtbetalingListenerMetricCounter.longValue
            every { repository.updateBatch(any(), any(), any(), any(), any(), any(), any()) } throws
                SQLException("SQLERRMC=00C90088;00000302", "57033", -913)

            callback.captured.onMessage(message)

            verify(exactly = 3) { repository.updateBatch(any(), any(), any(), any(), any(), any(), any()) }
            verify(exactly = 1) { producer.send(listOf(payload)) }
            verify(exactly = 1) { message.acknowledge() }
            verifyOrder {
                producer.send(listOf(payload))
                message.acknowledge()
            }
            Metrics.mqUtbetalingListenerMetricCounter.longValue shouldBe initialCount
            historyRepository.findAllByTransaksjonId(listOf(20025925, 20025926)).count {
                it.transaksjonTilstandType == TRANS_TILSTAND_OPPDRAG_RETUR_OK
            } shouldBe 0
        }

        test("does not retry unrelated database failures before BOQ delivery") {
            every { repository.updateBatch(any(), any(), any(), any(), any(), any(), any()) } throws
                SQLException("Duplicate key", "23505", -803)

            callback.captured.onMessage(message)

            verify(exactly = 1) { repository.updateBatch(any(), any(), any(), any(), any(), any(), any()) }
            verify(exactly = 1) { producer.send(listOf(payload)) }
            verify(exactly = 1) { message.acknowledge() }
        }
    })
