package no.nav.sokos.spk.mottak.security

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
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import no.nav.security.mock.oauth2.MockOAuth2Server
import no.nav.security.mock.oauth2.token.DefaultOAuth2TokenCallback
import no.nav.security.mock.oauth2.withMockOAuth2Server
import no.nav.sokos.spk.mottak.API_BASE_PATH
import no.nav.sokos.spk.mottak.api.spkApi
import no.nav.sokos.spk.mottak.config.AUTHENTICATION_NAME
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.authenticate
import no.nav.sokos.spk.mottak.config.configureSecurity
import no.nav.sokos.spk.mottak.configureTestApplication
import no.nav.sokos.spk.mottak.service.FileReaderService

val fileReaderService: FileReaderService = mockk()

class SecurityTest : FunSpec({

    test("test http GET endepunkt uten token bør returnere 401") {
        withMockOAuth2Server {
            testApplication {
                configureTestApplication()
                this.application {
                    configureSecurity(authConfig())
                    routing {
                        authenticate(true, AUTHENTICATION_NAME) {
                            spkApi(fileReaderService)
                        }
                    }
                }
                val response = client.get("$API_BASE_PATH/fetchFiles")
                response.status shouldBe HttpStatusCode.Unauthorized
            }
        }
    }

    test("test http GET endepunkt med token bør returnere 200") {
        withMockOAuth2Server {
            val mockOAuth2Server = this
            testApplication {
                val client = createClient {
                    install(ContentNegotiation) {
                        json(Json {
                            prettyPrint = true
                            ignoreUnknownKeys = true
                            encodeDefaults = true

                            @OptIn(ExperimentalSerializationApi::class)
                            explicitNulls = false
                        }
                        )
                    }
                }
                configureTestApplication()
                this.application {
                    configureSecurity(authConfig())
                    routing {
                        authenticate(true, AUTHENTICATION_NAME) {
                            spkApi(fileReaderService)
                        }
                    }
                }

                every { fileReaderService.readAndParseFile() } returns Unit

                val response = client.get("$API_BASE_PATH/fetchFiles") {
                    header("Authorization", "Bearer ${mockOAuth2Server.tokenFromDefaultProvider()}")
                    contentType(ContentType.Application.Json)
                }

                response.status shouldBe HttpStatusCode.OK
            }
        }
    }
})

private fun MockOAuth2Server.authConfig() =
    PropertiesConfig.AzureAdConfig(
        wellKnownUrl = wellKnownUrl("default").toString(),
        clientId = "default"
    )

private fun MockOAuth2Server.tokenFromDefaultProvider() =
    issueToken(
        issuerId = "default",
        clientId = "default",
        tokenCallback = DefaultOAuth2TokenCallback()
    ).serialize()