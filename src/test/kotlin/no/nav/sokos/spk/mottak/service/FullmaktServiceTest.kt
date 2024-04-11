package no.nav.sokos.spk.mottak.service

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock
import io.kotest.core.spec.style.FunSpec
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.kotest.matchers.shouldBe
import java.net.HttpURLConnection
import java.net.URL

class FullmaktServiceTest : FunSpec({
    val fullmaktServiceServer = WireMockServer(9000)
    listener(WireMockListener(fullmaktServiceServer, ListenerMode.PER_SPEC))

    test("skal returnere 200 OK ved kall") {
        fullmaktServiceServer.stubFor(
            WireMock.get(WireMock.urlEqualTo("/api/v1/fullmakter"))
                .willReturn(WireMock.ok())
        )

        val connection = URL("http://localhost:9000/api/v1/fullmakter").openConnection() as HttpURLConnection
        connection.responseCode shouldBe 200
    }
})