package no.nav.sokos.spk.mottak.service

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.WiremockTestConfig
import no.nav.sokos.spk.mottak.exception.FullmaktException
import no.nav.sokos.spk.mottak.integration.FullmaktClientService
import no.nav.sokos.spk.mottak.security.AccessTokenClient

private val accessTokenClient = mockk<AccessTokenClient>()

class FullmaktClientServiceTest : FunSpec({

    val fullmaktClientService = FullmaktClientService(
        pensjonFullmaktUrl = WiremockTestConfig.wiremock.baseUrl(),
        accessTokenClient = accessTokenClient
    )

    test("skal returnere fullmakter") {

        WiremockTestConfig.wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .willReturn(okJson("fullmakter.json".readFromResource()))
        )
        WiremockTestConfig.wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=1&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .willReturn(okJson("[]"))
        )
        coEvery { accessTokenClient.hentAccessToken() } returns "token"

        val fullmaktList = fullmaktClientService.getFullmakter()

        fullmaktList.size shouldBe 5
        fullmaktList shouldBe mapOf(
            "11528524674" to "10488337381",
            "04127604695" to "11058114091",
            "61128149685" to "07028229873",
            "19917199240" to "03085623833",
            "10410351752" to "11515509803"
        )
    }

/*    test("tester retry") {

        WiremockTestConfig.wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(STARTED)
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
                .willSetStateTo("Cause Failure")
        )

        println("Første stub fullført")

        WiremockTestConfig.wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Cause Failure")
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
                .willSetStateTo("Cause Success")
        )

        println("Andre stub fullført")

        WiremockTestConfig.wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Cause Success")
                .willReturn(okJson("fullmakter.json".readFromResource()))
        )

        println("Tredje stub fullført, 200 OK")


        coEvery { accessTokenClient.hentAccessToken() } returns "token"

        val actualFullmakter = fullmaktClientService.hentAlleFullmakter()
        actualFullmakter.size shouldBe 0
    }*/

    test("skal kaste exception ved feil mot kall til fullmakt") {

        WiremockTestConfig.wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
        )

        coEvery { accessTokenClient.hentAccessToken() } returns "token"

        shouldThrow<FullmaktException> {
            fullmaktClientService.getFullmakter()
        }.message shouldBe "Uforventet feil ved oppslag av fullmakter"
    }

})