package no.nav.sokos.spk.mottak.pdl

import com.expediagroup.graphql.client.types.GraphQLClientError
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import mu.KotlinLogging
import org.slf4j.MDC

import no.nav.pdl.HentIdenterBolk
import no.nav.pdl.hentidenterbolk.IdentInformasjon
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.httpClient
import no.nav.sokos.spk.mottak.security.AccessTokenClient

private val logger = KotlinLogging.logger {}

class PdlService(
    private val pdlUrl: String = PropertiesConfig.PdlProperties().pdlUrl,
    private val pdlScope: String = PropertiesConfig.PdlProperties().pdlScope,
    private val client: HttpClient = httpClient,
    private val accessTokenClient: AccessTokenClient = AccessTokenClient(azureAdScope = pdlScope),
) {
    suspend fun getIdenterBolk(identer: List<String>): Map<String, List<IdentInformasjon>> {
        val request = HentIdenterBolk(HentIdenterBolk.Variables(identer))

        logger.info { "Henter accesstoken for oppslag mot PDL" }
        val accessToken = accessTokenClient.getSystemToken()

        logger.info { "Henter identer fra PDL" }
        val response =
            client.post("$pdlUrl/graphql") {
                header(HttpHeaders.Authorization, "Bearer $accessToken")
                header("behandlingsnummer", "B154")
                header("Nav-Call-Id", MDC.get("x-correlation-id"))
                contentType(ContentType.Application.Json)
                setBody(request)
            }

        return when {
            response.status.isSuccess() -> {
                val result = response.body<GraphQLResponse<HentIdenterBolk.Result>>()
                if (result.errors?.isNotEmpty() == true) {
                    handleErrors(result.errors)
                }
                result.data
                    ?.hentIdenterBolk
                    ?.map { item ->
                        item.ident to (item.identer ?: emptyList())
                    }
                    ?.groupBy({ it.first }, { it.second })
                    ?.mapValues { entry -> entry.value.flatten() }.orEmpty()
            }

            else -> {
                throw ClientRequestException(
                    response,
                    "Noe gikk galt ved oppslag mot PDL",
                )
            }
        }
    }

    private fun handleErrors(errors: List<GraphQLClientError>) {
        val errorMessage = errors.joinToString { it.message }
        val exceptionMessage = "Message: $errorMessage"
        throw PdlException(
            exceptionMessage,
        )
    }
}

data class PdlException(
    override val message: String,
) : Exception(message)
