package no.nav.sokos.spk.mottak.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.equalTo
import com.github.tomakehurst.wiremock.client.WireMock.matching
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import io.ktor.util.Identity.encode
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotliquery.param
import no.nav.sokos.spk.mottak.integration.FullmaktClientService
import no.nav.sokos.spk.mottak.security.AccessTokenClient
import org.testcontainers.shaded.org.bouncycastle.util.encoders.UTF8

class FullmaktClientServiceTest : FunSpec({
    val fullmaktClientServiceServer = WireMockServer(9000)
    fullmaktClientServiceServer.start()
    listener(WireMockListener(fullmaktClientServiceServer, ListenerMode.PER_SPEC))
    val accessToken = mockk<AccessTokenClient>()
    val fullmaktClientService = FullmaktClientService(pensjonFullmaktUrl = fullmaktClientServiceServer.baseUrl(), accessTokenClient = accessToken)

    afterSpec {
        fullmaktClientServiceServer.stop()
    }

    test("skal returnere 200 OK ved kall") {
        fullmaktClientServiceServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/v1/fullmakter"))
                .willReturn(WireMock.ok())
        )

        val connection = URL("http://localhost:9000/api/v1/fullmakter").openConnection() as HttpURLConnection
        connection.responseCode shouldBe 200
    }

    test("should return a map") {
        val expectedResponse = """
        [
            {"aktorIdentGirFullmakt": "22031366171", "aktorIdentMottarFullmakt": "07846497913"},
            {"aktorIdentGirFullmakt": "02500260109", "aktorIdentMottarFullmakt": "08049011297"}
        ]
    """.trimIndent()
        fullmaktClientServiceServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/finnFullmaktMottakere?side=0&antall=1000&koderFullmaktType=PENGEMOT%2CVERGE_PENGEMOT"))
                //.withQueryParam("side", equalTo("1"))
                //.withQueryParam("antall", WireMock.equalTo("1000"))
                //.withQueryParam("koderFullmaktType", WireMock.equalTo("PENGEMOT,VERGE_PENGEMOT"))
                .willReturn(WireMock.okJson(expectedResponse))
        )

        coEvery { accessToken.hentAccessToken() } returns "token"
        val actualFullmakter = fullmaktClientService.hentAlleFullmakter()

        val expectedResult = mapOf(
            "61128149685" to "07028229873",
            "14105126167" to "15107225622",
            "12465420012" to "80000781720"
        )
        actualFullmakter shouldBe expectedResult
    }
})