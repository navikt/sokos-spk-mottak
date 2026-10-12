package no.nav.sokos.spk.mottak.config

import java.sql.Connection
import javax.sql.DataSource

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

internal class InternalNaisRoutesTest :
    FunSpec({
        test("readiness follows database failure and recovery without affecting liveness") {
            val state = ApplicationState(ready = true, alive = true)
            val db2 = mockk<DataSource>()
            val postgres = mockk<DataSource>()
            val db2Connection = mockk<Connection>(relaxed = true)
            val postgresConnection = mockk<Connection>(relaxed = true)
            every { db2.connection } returns db2Connection
            every { postgres.connection } returns postgresConnection
            every { db2Connection.isValid(1) } returns true
            every { postgresConnection.isValid(1) } returns true

            testApplication {
                application {
                    routing {
                        internalNaisRoutes(
                            applicationState = state,
                            readynessCheck = { state.ready && databasesHealthy(db2, postgres) },
                        )
                    }
                }

                client.get("/internal/isReady").status shouldBe HttpStatusCode.OK

                every { db2Connection.isValid(1) } returns false
                client.get("/internal/isReady").status shouldBe HttpStatusCode.ServiceUnavailable
                client.get("/internal/isAlive").status shouldBe HttpStatusCode.OK

                every { db2Connection.isValid(1) } returns true
                client.get("/internal/isReady").status shouldBe HttpStatusCode.OK

                every { postgresConnection.isValid(1) } returns false
                client.get("/internal/isReady").status shouldBe HttpStatusCode.ServiceUnavailable
                client.get("/internal/isAlive").status shouldBe HttpStatusCode.OK

                every { postgresConnection.isValid(1) } returns true
                client.get("/internal/isReady").status shouldBe HttpStatusCode.OK

                state.ready = false
                client.get("/internal/isReady").status shouldBe HttpStatusCode.ServiceUnavailable
                verify(exactly = 5) { db2.connection }
                verify(exactly = 5) { postgres.connection }
            }
        }

        test("default readiness and liveness follow application lifecycle") {
            val state = ApplicationState()
            testApplication {
                application {
                    routing { internalNaisRoutes(state) }
                }

                client.get("/internal/isReady").status shouldBe HttpStatusCode.ServiceUnavailable
                client.get("/internal/isAlive").status shouldBe HttpStatusCode.InternalServerError

                state.ready = true
                state.alive = true
                client.get("/internal/isReady").status shouldBe HttpStatusCode.OK
                client.get("/internal/isAlive").status shouldBe HttpStatusCode.OK
            }
        }
    })
