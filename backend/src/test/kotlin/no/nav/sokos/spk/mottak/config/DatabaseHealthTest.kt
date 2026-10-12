package no.nav.sokos.spk.mottak.config

import java.sql.Connection
import java.sql.SQLException
import javax.sql.DataSource

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

internal class DatabaseHealthTest :
    FunSpec({
        test("both databases must have valid connections") {
            for ((db2Healthy, postgresHealthy) in listOf(true to true, false to true, true to false, false to false)) {
                val db2Connection = mockk<Connection>(relaxed = true)
                val postgresConnection = mockk<Connection>(relaxed = true)
                val db2 = mockk<DataSource>()
                val postgres = mockk<DataSource>()
                every { db2.connection } returns db2Connection
                every { postgres.connection } returns postgresConnection
                every { db2Connection.isValid(1) } returns db2Healthy
                every { postgresConnection.isValid(1) } returns postgresHealthy

                databasesHealthy(db2, postgres) shouldBe (db2Healthy && postgresHealthy)

                verify(exactly = 1) { db2Connection.close() }
                verify(exactly = 1) { postgresConnection.close() }
            }
        }

        test("connection failures mark either database unhealthy") {
            for (db2Fails in listOf(true, false)) {
                val failing = mockk<DataSource>()
                val healthy = mockk<DataSource>()
                val connection = mockk<Connection>(relaxed = true)
                every { failing.connection } throws SQLException("Database unavailable", "08001")
                every { healthy.connection } returns connection
                every { connection.isValid(1) } returns true

                if (db2Fails) {
                    databasesHealthy(failing, healthy) shouldBe false
                } else {
                    databasesHealthy(healthy, failing) shouldBe false
                }

                verify(exactly = 1) { connection.close() }
            }
        }

        test("validation failures return connections to their pools") {
            val db2 = mockk<DataSource>()
            val postgres = mockk<DataSource>()
            val connection = mockk<Connection>(relaxed = true)
            val postgresConnection = mockk<Connection>(relaxed = true)
            every { db2.connection } returns connection
            every { postgres.connection } returns postgresConnection
            every { connection.isValid(1) } throws SQLException("Validation failed")
            every { postgresConnection.isValid(1) } returns true

            databasesHealthy(db2, postgres) shouldBe false

            verify(exactly = 1) { connection.close() }
            verify(exactly = 1) { postgresConnection.close() }
        }

        test("unexpected errors are not hidden as database failures") {
            val db2 = mockk<DataSource>()
            val postgres = mockk<DataSource>()
            val connection = mockk<Connection>(relaxed = true)
            every { db2.connection } throws IllegalStateException("Configuration error")
            every { postgres.connection } returns connection
            every { connection.isValid(1) } returns true

            shouldThrow<IllegalStateException> {
                databasesHealthy(db2, postgres)
            }
        }
    })
