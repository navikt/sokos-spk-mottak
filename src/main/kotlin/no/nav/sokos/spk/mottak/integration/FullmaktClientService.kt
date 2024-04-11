package no.nav.sokos.spk.mottak.integration

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.httpClient
import no.nav.sokos.spk.mottak.exception.FullmaktException
import no.nav.sokos.spk.mottak.integration.models.Fullmakt
import no.nav.sokos.spk.mottak.integration.models.FullmaktListe
import no.nav.sokos.spk.mottak.security.AccessTokenClient

private val logger = KotlinLogging.logger {}


class FullmaktClientService {
    private val pensjonFullmaktUrl: String = PropertiesConfig.PensjonFullmaktConfig().fullmaktUrl
    private val fullmaktHttpClient: HttpClient = httpClient
    private val accessTokenClient: AccessTokenClient = AccessTokenClient()

    suspend fun getFullMakter() {
        try {
            val accessToken = accessTokenClient.hentAccessToken()
            fullmaktHttpClient.get("$pensjonFullmaktUrl/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=VERGE,VERGE_PENGEMOT") {
                logger.info { "Kaller pensjon-fullmakt" }
                header("Authorization", "Bearer $accessToken")
            }.let { response ->
                when (response.status.value) {
                    200 -> response.body<FullmaktListe>()
                    else -> {
                        response.body<String>()
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error(ex) { "Feil ved henting av fullmakt" + ex.message }
            throw FullmaktException(ex)
        }
    }
}