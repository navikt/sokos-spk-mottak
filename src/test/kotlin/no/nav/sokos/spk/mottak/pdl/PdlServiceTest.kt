package no.nav.sokos.spk.mottak.pdl

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.common.ContentTypes.APPLICATION_JSON
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode

import no.nav.pdl.enums.IdentGruppe
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.listener.WiremockListener
import no.nav.sokos.spk.mottak.listener.WiremockListener.wiremock

internal class PdlServiceTest :
    FunSpec({

        extensions(listOf(WiremockListener))

        val pdlService: PdlService by lazy {
            PdlService(
                pdlUrl = wiremock.baseUrl(),
                accessTokenClient = WiremockListener.accessTokenClient,
            )
        }

        test("hent identer fra PDL gir respons med identer") {

            val identerFunnetOkResponse = readFromResource("/pdl/hentIdenterBolkOkResponse.json")

            wiremock.stubFor(
                WireMock
                    .post(urlEqualTo("/graphql"))
                    .willReturn(
                        aResponse()
                            .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                            .withStatus(HttpStatusCode.OK.value)
                            .withBody(identerFunnetOkResponse),
                    ),
            )

            val response = pdlService.getIdenterBolk(listOf("12345678912", "01111953488", "40074203226"))

            response.size shouldBe 3

            response["12345678912"]?.size shouldBe 2
            response["12345678912"]?.get(0)?.historisk shouldBe true
            response["12345678912"]?.get(1)?.historisk shouldBe false
            response["12345678912"]?.get(0)?.gruppe shouldBe IdentGruppe.FOLKEREGISTERIDENT

            response["01111953488"]?.size shouldBe 1
            response["01111953488"]?.get(0)?.historisk shouldBe false
            response["01111953488"]?.get(0)?.gruppe shouldBe IdentGruppe.FOLKEREGISTERIDENT

            response["40074203226"]?.size shouldBe 1
            response["40074203226"]?.get(0)?.historisk shouldBe false
            response["40074203226"]?.get(0)?.gruppe shouldBe IdentGruppe.FOLKEREGISTERIDENT
        }

        test("hent identer fra PDL med tom array request gir PdlException") {

            val identerFunnetFeilResponse = readFromResource("/pdl/hentIdenterBolkFeilResponse.json")

            wiremock.stubFor(
                WireMock
                    .post(urlEqualTo("/graphql"))
                    .willReturn(
                        aResponse()
                            .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                            .withStatus(HttpStatusCode.OK.value)
                            .withBody(identerFunnetFeilResponse),
                    ),
            )

            val exception =
                shouldThrow<PdlException> {
                    pdlService.getIdenterBolk(emptyList())
                }

            exception.message shouldBe "Message: Ingen identer angitt."
        }

        test("hent identer fra PDL uten accesstoken returnerer at clienten ikke er autentisert") {

            val ikkeAutentisertResponse = readFromResource("/pdl/ikkeAutentisertResponse.json")

            wiremock.stubFor(
                WireMock
                    .post(urlEqualTo("/graphql"))
                    .willReturn(
                        aResponse()
                            .withHeader(HttpHeaders.ContentType, APPLICATION_JSON)
                            .withStatus(HttpStatusCode.OK.value)
                            .withBody(ikkeAutentisertResponse),
                    ),
            )

            val exception =
                shouldThrow<PdlException> {
                    pdlService.getIdenterBolk(listOf("12345678912"))
                }

            exception.message shouldBe "Message: Ikke autentisert"
        }
    })
