package no.nav.sokos.spk.mottak

import io.ktor.server.config.MapApplicationConfig
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import no.nav.sokos.spk.mottak.api.naisApi
import no.nav.sokos.spk.mottak.config.commonConfig

internal const val API_BASE_PATH = "/mottak"

fun ApplicationTestBuilder.configureTestApplication() {
    val mapApplicationConfig = MapApplicationConfig()
    environment {
        config = mapApplicationConfig
    }

    application {
        commonConfig()
        val applicationState = ApplicationState(ready = true)

        routing {
            naisApi({ applicationState.initialized }, { applicationState.running })
        }
    }
}
