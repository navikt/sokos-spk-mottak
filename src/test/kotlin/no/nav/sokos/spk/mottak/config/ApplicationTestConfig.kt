package no.nav.sokos.spk.mottak.config

import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.sokos.spk.mottak.api.naisApi

internal const val API_BASE_PATH = "/api/v1"

fun ApplicationTestBuilder.configureTestApplication() {
    val mapApplicationConfig = MapApplicationConfig()
    environment {
        config = mapApplicationConfig
    }

    application {
        val applicationState = ApplicationState()

        commonConfig()
        routing {
            naisApi(applicationState)
        }
    }
}
