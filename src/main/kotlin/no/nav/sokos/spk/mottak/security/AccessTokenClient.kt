package no.nav.sokos.spk.mottak.security

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.accept
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.http.ContentType
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.Parameters

import java.time.Instant
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.httpClient
import no.nav.sokos.spk.mottak.util.retry

private val logger = KotlinLogging.logger {}

class AccessTokenClient(
    private val azureAdConfig: PropertiesConfig.AzureAdConfig = PropertiesConfig.AzureAdConfig(),
    private val fullmaktConfig: PropertiesConfig.PensjonFullmaktConfig = PropertiesConfig.PensjonFullmaktConfig(),
    private val client: HttpClient = httpClient,
    private val adAccessTokenUrl: String = "https://login.microsoftonline.com/${azureAdConfig.tenantId}/oauth2/v2.0/token"
) {
    private val mutex = Mutex()

    @Volatile
    private var token: AccessToken = runBlocking { AccessToken(hentAccessTokenFraProvider()) }
    suspend fun hentAccessToken(): String {
        val omToMinutter = Instant.now().plusSeconds(120L)
        return mutex.withLock {
            when {
                token.expiresAt.isBefore(omToMinutter) -> {
                    logger.info("Henter ny accesstoken")
                    token = AccessToken(hentAccessTokenFraProvider())
                    token.accessToken
                }
                else -> token.accessToken
            }
        }
    }

    private suspend fun hentAccessTokenFraProvider(): AzureAccessToken =
        retry {
            val response: HttpResponse = client.post(adAccessTokenUrl) {
                accept(ContentType.Application.Json)
                method = HttpMethod.Post
                setBody(FormDataContent(Parameters.build {
                    append("tenant", azureAdConfig.tenantId)
                    append("client_id", azureAdConfig.clientId)
                    append("scope", "api://${fullmaktConfig.fullmaktScope}/.default")
                    append("client_secret", azureAdConfig.clientSecret)
                    append("grant_type", "client_credentials")
                }))
            }
            if (response.status != HttpStatusCode.OK) {
                val feilmelding =
                    "Kunne ikke hente accesstoken fra Azure. Statuskode: ${response.status}"
                logger.error { feilmelding }
                throw RuntimeException(feilmelding)
            } else {
                response.body()
            }
        }
}

@Serializable
private data class AzureAccessToken(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_in")
    val expiresIn: Long
)

private data class AccessToken(
    val accessToken: String,
    val expiresAt: Instant
) {
    constructor(azureAccessToken: AzureAccessToken) : this(
        accessToken = azureAccessToken.accessToken,
        expiresAt = Instant.now().plusSeconds(azureAccessToken.expiresIn)
    )
}