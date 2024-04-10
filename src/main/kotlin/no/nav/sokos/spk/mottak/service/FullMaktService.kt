package no.nav.sokos.spk.mottak.service

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.httpClient
import no.nav.sokos.spk.mottak.exception.FullmaktException
import no.nav.sokos.spk.mottak.security.AccessTokenClient
import org.slf4j.MDC

private val logger = KotlinLogging.logger{}


class FullMaktService {
    private val fullmaktUrl: String = PropertiesConfig.PensjonFullmaktConfig().fullmaktUrl
    private val fullmaktHttpClient: HttpClient = httpClient
    private val accessTokenClient: AccessTokenClient = AccessTokenClient()

    suspend fun getFullMakter() {
        try {
            val accessToken = accessTokenClient.hentAccessToken()
            fullmaktHttpClient.get("$fullmaktUrl/finnFullmaktMottakere") {
                logger.info { "Kaller pensjon-fullmakt" }
                header("Authorization", "Bearer $accessToken")
            }.body<String>()
        } catch (ex: Exception) {
            logger.error (ex) { "Feil ved henting av fullmakt" + ex.message }
            throw FullmaktException(ex)
        }
    }
}