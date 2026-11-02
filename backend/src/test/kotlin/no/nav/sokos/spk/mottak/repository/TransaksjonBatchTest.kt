package no.nav.sokos.spk.mottak.repository

import java.util.UUID

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotliquery.Session
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.DatabaseTestConfig
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_OK
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

internal class TransaksjonBatchTest :
    FunSpec({
        test("updates transactions in ID order while preserving their history ID association") {
            val dataSource = mockk<HikariDataSource>()
            val session = mockk<Session>()
            val parameters = slot<List<Map<String, Any?>>>()
            every { session.batchPreparedNamedStatement(any(), capture(parameters)) } returns listOf(1, 1, 1)

            TransaksjonRepository(dataSource).updateBatch(
                transaksjonIdList = listOf(30, 10, 20),
                transTilstandIdList = listOf(300, 100, 200),
                transaksjonTilstandType = TRANS_TILSTAND_OPPDRAG_RETUR_OK,
                systemId = "test",
                osStatus = "00",
                session = session,
            )

            parameters.captured.map { it["transaksjonId"] to it["transTilstandId"] } shouldBe
                listOf(10 to 100, 20 to 200, 30 to 300)
            parameters.captured.map { it["osStatus"] } shouldBe listOf("00", "00", "00")
        }

        test("rejects mismatched history IDs instead of silently truncating the update batch") {
            val session = mockk<Session>()

            shouldThrow<IllegalArgumentException> {
                TransaksjonRepository(mockk()).updateBatch(
                    transaksjonIdList = listOf(10, 20),
                    transTilstandIdList = listOf(100),
                    transaksjonTilstandType = TRANS_TILSTAND_OPPDRAG_RETUR_OK,
                    systemId = "test",
                    session = session,
                )
            }

            verify(exactly = 0) { session.batchPreparedNamedStatement(any(), any()) }
        }

        test("inserts history in ID order but returns history IDs in the caller's order") {
            HikariDataSource(
                DatabaseTestConfig.hikariConfig().apply {
                    jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};MODE=DB2"
                },
            ).use { dataSource ->
                dataSource.transaction { session ->
                    session.update(queryOf(readFromResource("/database/db2Script.sql")))
                }
                val repository = TransaksjonTilstandRepository(dataSource)

                dataSource.transaction { session ->
                    val transactionIds = listOf(30, 10, 20)
                    val historyIds =
                        repository.insertBatch(
                            transaksjonIdList = transactionIds,
                            transaksjonTilstandType = TRANS_TILSTAND_OPPDRAG_RETUR_OK,
                            systemId = "test",
                            session = session,
                        )
                    val storedRows =
                        session.list(queryOf("SELECT TRANSAKSJON_ID, TRANS_TILSTAND_ID FROM T_TRANS_TILSTAND ORDER BY TRANS_TILSTAND_ID")) {
                            it.int("TRANSAKSJON_ID") to it.int("TRANS_TILSTAND_ID")
                        }

                    storedRows.map { it.first } shouldBe listOf(10, 20, 30)
                    transactionIds.zip(historyIds).toMap() shouldBe storedRows.toMap()
                }
            }
        }
    })
