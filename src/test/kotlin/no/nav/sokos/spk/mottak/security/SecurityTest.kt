package no.nav.sokos.spk.mottak.security

import kotlinx.serialization.json.Json

import com.github.kagkarlsson.scheduler.Scheduler
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk

import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import no.nav.sokos.spk.mottak.api.API_BASE_PATH
import no.nav.sokos.spk.mottak.api.mottakApi
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.commonConfig
import no.nav.sokos.spk.mottak.config.securityConfig
import no.nav.sokos.spk.mottak.service.LeveAttestService
import no.nav.sokos.spk.mottak.service.ScheduledTaskService

internal class SecurityTest :
    FunSpec({
        val scheduler = mockk<Scheduler>()
        val scheduledTaskService = mockk<ScheduledTaskService>()
        val leveAttestService = mockk<LeveAttestService>()

        context("Authentication - Token Validation (401 Unauthorized)") {
            test("GET endpoint uten token skal returnere 401 Unauthorized") {
                withMockOAuth2Server {
                    testApplication {
                        application {
                            securityConfig(true, authConfig())
                            routing {
                                mottakApi(scheduler, scheduledTaskService, leveAttestService)
                            }
                        }
                        val response = client.get("$API_BASE_PATH/jobTaskInfo")
                        response.status shouldBe HttpStatusCode.Unauthorized
                    }
                }
            }

            test("GET endpoint med ugyldig token skal returnere 401 Unauthorized") {
                withMockOAuth2Server {
                    testApplication {
                        application {
                            securityConfig(true, authConfig())
                            routing {
                                mottakApi(scheduler, scheduledTaskService, leveAttestService)
                            }
                        }
                        val response =
                            client.get("$API_BASE_PATH/jobTaskInfo") {
                                header("Authorization", "Bearer invalid-token")
                                contentType(ContentType.Application.Json)
                            }
                        response.status shouldBe HttpStatusCode.Unauthorized
                    }
                }
            }
        }

        context("Authorization - OBO Pattern (requireScope)") {
            test("GET endpoint med gyldig OBO token men uten required scope skal returnere 403 Forbidden") {
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
                                mottakApi(scheduler, scheduledTaskService, leveAttestService)
                            }
                        }

                        val response =
                            client.get("$API_BASE_PATH/jobTaskInfo") {
                                header("Authorization", "Bearer ${mockOAuth2Server.oboTokenWithoutRequiredScope()}")
                                contentType(ContentType.Application.Json)
                            }

                        response.status shouldBe HttpStatusCode.Forbidden
                        val body = response.bodyAsText()
                        body shouldContain "Forbidden"
                        body shouldContain "Missing required scope"
                    }
                }
            }

            test("GET endpoint med gyldig OBO token og required scope skal returnere 200 OK") {
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
                                mottakApi(scheduler, scheduledTaskService, leveAttestService)
                            }
                        }

                        val response =
                            client.get("$API_BASE_PATH/jobTaskInfo") {
                                header("Authorization", "Bearer ${mockOAuth2Server.oboTokenWithRequiredScope()}")
                                contentType(ContentType.Application.Json)
                            }

                        response.status shouldBe HttpStatusCode.OK
                    }
                }
            }
        }

        context("Authorization - M2M Pattern (requireRole)") {
            test("M2M endpoint med M2M token uten required role skal returnere 403 Forbidden") {
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
                                mottakApi(scheduler, scheduledTaskService, leveAttestService)
                            }
                        }

                        val response =
                            client.get("$API_BASE_PATH/leveattester/2024-01-01") {
                                header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithoutRequiredRole()}")
                                contentType(ContentType.Application.Json)
                            }

                        response.status shouldBe HttpStatusCode.Forbidden
                        val body = response.bodyAsText()
                        body shouldContain "Forbidden"
                        body shouldContain "Missing required role"
                    }
                }
            }

            test("M2M endpoint med M2M token og required role skal returnere 200 OK") {
                every { leveAttestService.getLeveAttester(any()) } returns emptyList()

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
                                mottakApi(scheduler, scheduledTaskService, leveAttestService)
                            }
                        }

                        val response =
                            client.get("$API_BASE_PATH/leveattester/2024-01-01") {
                                header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithRequiredRole()}")
                                contentType(ContentType.Application.Json)
                            }

                        response.status shouldBe HttpStatusCode.OK
                    }
                }
            }
        }

        context("Authorization - Cross-Contamination (Token Separation)") {
            test("OBO token på M2M-only endpoint skal returnere 403 Forbidden") {
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
                                mottakApi(scheduler, scheduledTaskService, leveAttestService)
                            }
                        }

                        val response =
                            client.get("$API_BASE_PATH/leveattester/2024-01-01") {
                                header("Authorization", "Bearer ${mockOAuth2Server.oboTokenWithRequiredScope()}")
                                contentType(ContentType.Application.Json)
                            }

                        response.status shouldBe HttpStatusCode.Forbidden
                        val body = response.bodyAsText()
                        body shouldContain "Forbidden"
                        body shouldContain "Missing required role"
                    }
                }
            }

            test("M2M token på OBO-only endpoint skal returnere 403 Forbidden") {
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
                                mottakApi(scheduler, scheduledTaskService, leveAttestService)
                            }
                        }

                        val response =
                            client.get("$API_BASE_PATH/jobTaskInfo") {
                                header("Authorization", "Bearer ${mockOAuth2Server.m2mTokenWithRequiredRole()}")
                                contentType(ContentType.Application.Json)
                            }

                        response.status shouldBe HttpStatusCode.Forbidden
                        val body = response.bodyAsText()
                        body shouldContain "Forbidden"
                        body shouldContain "Missing required scope"
                    }
                }
            }
        }
    })

private fun MockOAuth2Server.authConfig() =
    PropertiesConfig.AzureAdProperties(
        wellKnownUrl = wellKnownUrl("default").toString(),
        clientId = "default",
    )

// OBO token with NAVident and required scope
private fun MockOAuth2Server.oboTokenWithRequiredScope() =
    issueToken(
        issuerId = "default",
        clientId = "default",
        tokenCallback =
            DefaultOAuth2TokenCallback(
                issuerId = "default",
                claims =
                    mapOf(
                        "NAVident" to "X123456",
                        "scp" to Scope.SPK_MOTTAK_ADMIN.value,
                    ),
            ),
    ).serialize()

// OBO token with NAVident but WITHOUT required scope
private fun MockOAuth2Server.oboTokenWithoutRequiredScope() =
    issueToken(
        issuerId = "default",
        clientId = "default",
        tokenCallback =
            DefaultOAuth2TokenCallback(
                issuerId = "default",
                claims =
                    mapOf(
                        "NAVident" to "X123456",
                        "scp" to "other.scope",
                    ),
            ),
    ).serialize()

// M2M token with required role (no NAVident)
private fun MockOAuth2Server.m2mTokenWithRequiredRole() =
    issueToken(
        issuerId = "default",
        clientId = "default",
        tokenCallback =
            DefaultOAuth2TokenCallback(
                issuerId = "default",
                claims =
                    mapOf(
                        "roles" to listOf(Role.LEVEATTESTER_READ.value),
                    ),
            ),
    ).serialize()

// M2M token WITHOUT required role
private fun MockOAuth2Server.m2mTokenWithoutRequiredRole() =
    issueToken(
        issuerId = "default",
        clientId = "default",
        tokenCallback =
            DefaultOAuth2TokenCallback(
                issuerId = "default",
                claims =
                    mapOf(
                        "roles" to listOf("other.role"),
                    ),
            ),
    ).serialize()
