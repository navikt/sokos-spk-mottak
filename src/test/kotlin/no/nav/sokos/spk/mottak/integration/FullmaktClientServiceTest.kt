package no.nav.sokos.spk.mottak.integration

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.stubbing.Scenario
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpStatusCode
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.WiremockTestConfig.wiremock
import no.nav.sokos.spk.mottak.exception.FullmaktException
import no.nav.sokos.spk.mottak.security.AccessTokenClient

private val accessTokenClient = mockk<AccessTokenClient>()

@Ignored
class FullmaktClientServiceTest : FunSpec({

    val fullmaktClientService = FullmaktClientService(
        pensjonFullmaktUrl = wiremock.baseUrl(),
        accessTokenClient = accessTokenClient
    )

    test("skal returnere fullmakter") {

        wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .willReturn(okJson(readFromResource("/fullmakter.json")))
        )
        wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=1&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .willReturn(okJson("[]"))
        )
        coEvery { accessTokenClient.getAccessToken() } returns "token"

        val fullmaktMap = fullmaktClientService.getFullmakt()

        fullmaktMap.size shouldBe 5
        fullmaktMap shouldBe mapOf(
            "11528524674" to "10488337381",
            "04127604695" to "11058114091",
            "61128149685" to "07028229873",
            "19917199240" to "03085623833",
            "10410351752" to "11515509803"
        )
    }

    test("tester retry når pensjon-fullmakt api returnerer 500 feil") {

        wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs(Scenario.STARTED)
                .willSetStateTo("Success")
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
        )

        wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Success")
                .willSetStateTo("Finish")
                .willReturn(okJson(readFromResource("/fullmakter.json")))
        )

        wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=1&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .inScenario("Retry Scenario")
                .whenScenarioStateIs("Finish")
                .willReturn(okJson("[]"))
        )

        coEvery { accessTokenClient.getAccessToken() } returns "token"

        val actualFullmakter = fullmaktClientService.getFullmakt()
        actualFullmakter.size shouldBe 5
    }

    test("skal kaste exception ved feil mot kall til pensjon-fullmakt api når api er nede") {

        wiremock.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .willReturn(
                    aResponse()
                        .withStatus(HttpStatusCode.InternalServerError.value)
                )
        )

        coEvery { accessTokenClient.getAccessToken() } returns "token"

        shouldThrow<FullmaktException> {
            fullmaktClientService.getFullmakt()
        }.message shouldBe "Uforventet feil ved oppslag av fullmakter, statuscode: 500"
    }
})