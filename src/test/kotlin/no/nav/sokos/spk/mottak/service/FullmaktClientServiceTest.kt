package no.nav.sokos.spk.mottak.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.integration.FullmaktClientService
import no.nav.sokos.spk.mottak.security.AccessTokenClient

val accessToken = mockk<AccessTokenClient>()

class FullmaktClientServiceTest : FunSpec({

    val fullmaktClientServiceServer = WireMockServer(9000)
    fullmaktClientServiceServer.start()
    listener(WireMockListener(fullmaktClientServiceServer, ListenerMode.PER_SPEC))
    val fullmaktClientService = FullmaktClientService(pensjonFullmaktUrl = fullmaktClientServiceServer.baseUrl(), accessTokenClient = accessToken)

    afterSpec {
        fullmaktClientServiceServer.stop()
    }

    test("skal returnere fullmakter") {

        fullmaktClientServiceServer.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .willReturn(okJson("fullmakter.json".readFromResource()))
        )
        fullmaktClientServiceServer.stubFor(
            get(urlEqualTo("/finnFullmaktMottakere?side=1&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                .willReturn(okJson("[]"))
        )
        coEvery { accessToken.hentAccessToken() } returns "token"

        val actualFullmakter = fullmaktClientService.hentAlleFullmakter()
        val expectedResult = mapOf(
            "11528524674" to "10488337381",
            "04127604695" to "11058114091",
            "61128149685" to "07028229873",
            "19917199240" to "03085623833",
            "10410351752" to "11515509803"
        )

        actualFullmakter.size shouldBe 5
        actualFullmakter shouldBe expectedResult
    }

})