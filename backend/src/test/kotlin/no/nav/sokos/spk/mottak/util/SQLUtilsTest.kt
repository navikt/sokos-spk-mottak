package no.nav.sokos.spk.mottak.util

import java.sql.BatchUpdateException
import java.sql.SQLException
import java.util.UUID

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.util.SQLUtils.transaction
import no.nav.sokos.spk.mottak.util.SQLUtils.transactionWithDeadlockRetry

internal class SQLUtilsTest :
    FunSpec({
        val deadlocks =
            mapOf<String, () -> SQLException>(
                "SQLCODE -913" to { deadlock() },
                "SQLCODE -911" to { SQLException("SQLERRMC=00C90088;00000302", "40001", -911) },
                "batch exception chain" to {
                    BatchUpdateException("Batch failed", null, -4229, intArrayOf()).apply {
                        setNextException(deadlock())
                    }
                },
                "nested cause" to { SQLException("Database operation failed", deadlock()) },
            )

        deadlocks.forEach { (description, exception) ->
            test("rolls back the whole transaction before retrying $description") {
                retryDataSource().use { dataSource ->
                    var attempts = 0
                    val result =
                        dataSource.transactionWithDeadlockRetry { session ->
                            attempts++
                            session.update(queryOf("INSERT INTO RETRY_TEST (ID) VALUES (?)", attempts))
                            if (attempts < 3) throw exception()
                            "committed"
                        }

                    result shouldBe "committed"
                    attempts shouldBe 3
                    dataSource.transaction { session ->
                        session.list(queryOf("SELECT ID FROM RETRY_TEST")) { it.int("ID") }
                    } shouldBe listOf(3)
                }
            }
        }

        test("does not retry a successful transaction or change its nullable result") {
            retryDataSource().use { dataSource ->
                var attempts = 0
                val result =
                    dataSource.transactionWithDeadlockRetry {
                        attempts++
                        null
                    }

                attempts shouldBe 1
                result shouldBe null
            }
        }

        val nonRetryableErrors =
            mapOf(
                "timeout" to SQLException("SQLERRMC=00C9008E;00000302", "57033", -913),
                "unclassified rollback" to SQLException("Rollback", "40001", -911),
                "constraint violation" to SQLException("Duplicate key", "23505", -803),
                "other vendor code" to SQLException("00C90088", "57033", -803),
                "application failure" to IllegalStateException("Application failure"),
            )

        nonRetryableErrors.forEach { (description, failure) ->
            test("rolls back without retrying $description") {
                retryDataSource().use { dataSource ->
                    var attempts = 0
                    val thrown =
                        shouldThrow<Exception> {
                            dataSource.transactionWithDeadlockRetry { session ->
                                attempts++
                                session.update(queryOf("INSERT INTO RETRY_TEST (ID) VALUES (?)", attempts))
                                throw failure
                            }
                        }

                    thrown shouldBe failure
                    attempts shouldBe 1
                    dataSource.transaction { session ->
                        session.list(queryOf("SELECT ID FROM RETRY_TEST")) { it.int("ID") }
                    } shouldBe emptyList()
                }
            }
        }

        test("propagates the last deadlock after three rolled-back attempts") {
            retryDataSource().use { dataSource ->
                var attempts = 0
                val failure = deadlock()
                val thrown =
                    shouldThrow<SQLException> {
                        dataSource.transactionWithDeadlockRetry { session ->
                            attempts++
                            session.update(queryOf("INSERT INTO RETRY_TEST (ID) VALUES (?)", attempts))
                            throw failure
                        }
                    }

                thrown shouldBe failure
                attempts shouldBe 3
                dataSource.transaction { session ->
                    session.list(queryOf("SELECT ID FROM RETRY_TEST")) { it.int("ID") }
                } shouldBe emptyList()
            }
        }

        test("stops retrying when a later attempt fails for another reason") {
            retryDataSource().use { dataSource ->
                var attempts = 0
                val failure = SQLException("Duplicate key", "23505", -803)
                val thrown =
                    shouldThrow<SQLException> {
                        dataSource.transactionWithDeadlockRetry {
                            attempts++
                            if (attempts == 1) throw deadlock()
                            throw failure
                        }
                    }

                thrown shouldBe failure
                attempts shouldBe 2
            }
        }
    })

private fun deadlock() = SQLException("SQLERRMC=00C90088;00000302;MO611P.STRNSJ.X'3723D2'", "57033", -913)

private fun retryDataSource(): HikariDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:${UUID.randomUUID()};MODE=DB2"
            maximumPoolSize = 1
        },
    ).apply {
        transaction { session ->
            session.update(queryOf("CREATE TABLE RETRY_TEST (ID INTEGER PRIMARY KEY)"))
        }
    }
