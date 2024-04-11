package no.nav.sokos.spk.mottak.integration

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.httpClient
import no.nav.sokos.spk.mottak.exception.FullmaktException
import no.nav.sokos.spk.mottak.integration.models.FullmaktDTO
import no.nav.sokos.spk.mottak.security.AccessTokenClient
import no.nav.sokos.spk.mottak.util.retry

private val logger = KotlinLogging.logger {}
private const val KODER_FULLMAKT_TYPE = "PENGEMOT,VERGE_PENGEMOT"

class FullmaktClientService(
    private val pensjonFullmaktUrl: String = PropertiesConfig.PensjonFullmaktConfig().fullmaktUrl,
    private val fullmaktHttpClient: HttpClient = httpClient,
    private val accessTokenClient: AccessTokenClient = AccessTokenClient()
) {

    fun hentAlleFullmakter(): Map<String, String> {
        var side = 0
        val antall = 1000
        val fullmaktMap = mutableMapOf<String, String>()
        while (true) {
            val fullmakter = getFullMakter(side, antall)
            when {
                fullmakter.isNotEmpty() -> {
                    fullmaktMap.putAll(fullmakter.map { (it.aktorIdentGirFullmakt to it.aktorIdentMottarFullmakt) })
                    side++
                }

                else -> break
            }
        }
        logger.info { "Returnerer ${fullmaktMap.size} fullmakter" }
        return fullmaktMap
    }

    private fun getFullMakter(side: Int, antall: Int): List<FullmaktDTO> =
        runBlocking {
            retry {
                logger.debug { "Henter fullmakter" }
                val accessToken = accessTokenClient.hentAccessToken()
                fullmaktHttpClient.get("$pensjonFullmaktUrl/finnFullmaktMottakere") {
                    header("Authorization", "Bearer $accessToken")
                    parameter("side", side)
                    parameter("antall", antall)
                    parameter("koderFullmaktType", KODER_FULLMAKT_TYPE)
                }
            }.let { response ->
                when (response.status.value) {
                    200 -> {
                        response.body<List<FullmaktDTO>>()
                    }

                    else -> {
                        logger.error { "Uforventet feil ved oppslag av fullmakter. Statuskode: ${response.status.value}" }
                        throw FullmaktException(
                            response.status.value.toString(),
                            "Uforventet feil ved oppslag av fullmakter"
                        )
                    }
                }
            }
        }
}
