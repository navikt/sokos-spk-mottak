package no.nav.sokos.spk.mottak.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.integration.FullmaktClientService
import no.nav.sokos.spk.mottak.security.AccessTokenClient

class FullmaktClientServiceTest : FunSpec({
    val fullmaktClientServiceServer = WireMockServer(9000)
    fullmaktClientServiceServer.start()
    listener(WireMockListener(fullmaktClientServiceServer, ListenerMode.PER_SPEC))
    val accessToken = mockk<AccessTokenClient>()
    val fullmaktClientService = FullmaktClientService(pensjonFullmaktUrl = fullmaktClientServiceServer.baseUrl(), accessTokenClient = accessToken)

    afterSpec {
        fullmaktClientServiceServer.stop()
    }

    test("skal returnere riktig antall fullmakter") {

        fullmaktClientServiceServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                //.withQueryParam("side", equalTo("1"))
                //.withQueryParam("antall", WireMock.equalTo("1000"))
                //.withQueryParam("koderFullmaktType", WireMock.equalTo("PENGEMOT,VERGE_PENGEMOT"))
                .willReturn(WireMock.okJson("fullmakter.json".readFromResource()))
        )

        fullmaktClientServiceServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/finnFullmaktMottakere?side=1&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                //.withQueryParam("side", equalTo("1"))
                //.withQueryParam("antall", WireMock.equalTo("1000"))
                //.withQueryParam("koderFullmaktType", WireMock.equalTo("PENGEMOT,VERGE_PENGEMOT"))
                .willReturn(WireMock.okJson("[]"))
        )

        coEvery { accessToken.hentAccessToken() } returns "token"
        val actualFullmakter = fullmaktClientService.hentAlleFullmakter()

        actualFullmakter.size shouldBe 95
    }
})