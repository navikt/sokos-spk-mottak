package no.nav.sokos.spk.mottak.integration

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.isSuccess
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.httpClient
import no.nav.sokos.spk.mottak.exception.FullmaktException
import no.nav.sokos.spk.mottak.integration.models.FullmaktDTO
import no.nav.sokos.spk.mottak.security.AccessTokenClient
import no.nav.sokos.spk.mottak.util.retry

private val logger = KotlinLogging.logger {}

class FullmaktClientService(
    private val pensjonFullmaktUrl: String = PropertiesConfig.PensjonFullmaktConfig().fullmaktUrl,
    private val fullmaktHttpClient: HttpClient = httpClient,
    private val accessTokenClient: AccessTokenClient = AccessTokenClient()
) {

    suspend fun getFullmakter(): Map<String, String> {
        var side = 0
        val antall = 1000
        val fullmaktMap = mutableMapOf<String, String>()
        while (true) {
            val fullmaktList = getFullmaktMottakere(side, antall)
            when {
                fullmaktList.isNotEmpty() -> {
                    fullmaktMap.putAll(fullmaktList.map { (it.aktorIdentGirFullmakt to it.aktorIdentMottarFullmakt) })
                    side++
                }

                else -> break
            }
        }
        logger.info { "Returnerer ${fullmaktMap.size} fullmakter" }
        return fullmaktMap
    }

    private suspend fun getFullmaktMottakere(side: Int, antall: Int): List<FullmaktDTO> =
        retry {
            logger.debug { "Henter fullmakter" }
            val accessToken = accessTokenClient.hentAccessToken()
            val response = fullmaktHttpClient.get("$pensjonFullmaktUrl/finnFullmaktMottakere") {
                header("Authorization", "Bearer $accessToken")
                parameter("side", side)
                parameter("antall", antall)
                parameter("koderFullmaktType", "PENGEMOT,VERGE_PENGEMOT")
            }

            when {
                response.status.isSuccess() -> response.body<List<FullmaktDTO>>()
                else -> {
                    logger.error { "Uforventet feil ved oppslag av fullmakter. Statuskode: ${response.status.value}" }
                    throw FullmaktException("Uforventet feil ved oppslag av fullmakter, statuscode: ${response.status.value}")
                }
            }
        }

}
