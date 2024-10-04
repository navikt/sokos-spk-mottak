package no.nav.sokos.spk.mottak.listener

import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.configureFor
import io.kotest.core.config.AbstractProjectConfig
import io.kotest.extensions.wiremock.ListenerMode
import io.kotest.extensions.wiremock.WireMockListener
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.sokos.spk.mottak.security.AccessTokenClient

private const val WIREMOCK_SERVER_PORT = 9001

object WiremockListener : AbstractProjectConfig() {
    val wiremock = WireMockServer(WIREMOCK_SERVER_PORT)
    val accessTokenClient = mockk<AccessTokenClient>()

    private val wiremockListener = WireMockListener(wiremock, ListenerMode.PER_PROJECT)

    override fun extensions() = listOf(wiremockListener)

    override suspend fun beforeProject() {
        configureFor(WIREMOCK_SERVER_PORT)
        coEvery { accessTokenClient.getSystemToken() } returns "token"
    }
}
