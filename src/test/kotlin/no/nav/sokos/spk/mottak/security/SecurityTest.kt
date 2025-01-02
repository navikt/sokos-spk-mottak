package no.nav.sokos.spk.mottak.security

import com.github.kagkarlsson.scheduler.Scheduler
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlinx.serialization.json.Json
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import no.nav.sokos.spk.mottak.api.mottakApi
import no.nav.sokos.spk.mottak.config.AUTHENTICATION_NAME
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.authenticate
import no.nav.sokos.spk.mottak.config.commonConfig
import no.nav.sokos.spk.mottak.config.securityConfig
import no.nav.sokos.spk.mottak.service.ScheduledTaskService

private const val API_BASE_PATH = "/api/v1"

internal class SecurityTest :
    FunSpec({
        val scheduler = mockk<Scheduler>()
        val scheduledTaskService = mockk<ScheduledTaskService>()

        test("http GET endepunkt uten token bør returnere 401 - Unauthorized") {
            withMockOAuth2Server {
                testApplication {
                    application {
                        securityConfig(true, authConfig())
                        routing {
                            authenticate(true, AUTHENTICATION_NAME) {
                                mottakApi(scheduler, scheduledTaskService)
                            }
                        }
                    }
                    val response = client.get("$API_BASE_PATH/jobTaskInfo")
                    response.status shouldBe HttpStatusCode.Unauthorized
                }
            }
        }

        test("http GET endepunkt med token bør returnere 200 - OK") {
            every { scheduledTaskService.getScheduledTaskInformation() } returns emptyList()

            withMockOAuth2Server {
                val mockOAuth2Server = this
                testApplication {
                    val client =
                        createClient {
                            install(ContentNegotiation) {
                                json(
                                    Json {
                                        prettyPrint = true
                                        ignoreUnknownKeys = true
                                        encodeDefaults = true
                                        explicitNulls = false
                                    },
                                )
                            }
                        }
                    application {
                        commonConfig()
                        securityConfig(true, authConfig())
                        routing {
                            authenticate(true, AUTHENTICATION_NAME) {
                                mottakApi(scheduler, scheduledTaskService)
                            }
                        }
                    }

                    val response =
                        client.get("$API_BASE_PATH/jobTaskInfo") {
                            header("Authorization", "Bearer ${mockOAuth2Server.tokenFromDefaultProvider()}")
                            contentType(ContentType.Application.Json)
                        }

                    response.status shouldBe HttpStatusCode.OK
                }
            }
        }
    })

private fun MockOAuth2Server.authConfig() =
    PropertiesConfig.AzureAdProperties(
        wellKnownUrl = wellKnownUrl("default").toString(),
        clientId = "default",
    )

private fun MockOAuth2Server.tokenFromDefaultProvider() =
    issueToken(
        issuerId = "default",
        clientId = "default",
        tokenCallback = DefaultOAuth2TokenCallback(),
    ).serialize()
