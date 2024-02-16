package no.nav.sokos.spk.mottak.config

import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.sokos.spk.mottak.ApplicationState
import no.nav.sokos.spk.mottak.api.dummyApi
import no.nav.sokos.spk.mottak.api.metricsApi
import no.nav.sokos.spk.mottak.api.naisApi
import no.nav.sokos.spk.mottak.api.swaggerApi

fun Application.routingConfig(
    applicationState: ApplicationState,
    useAuthentication: Boolean
) {
    routing {
        naisApi({ applicationState.initialized }, { applicationState.running })
        metricsApi()
        swaggerApi()
        authenticate(useAuthentication, AUTHENTICATION_NAME) {
            dummyApi()
        }
    }
}

fun Route.authenticate(useAuthentication: Boolean, authenticationProviderId: String? = null, block: Route.() -> Unit) {
    if (useAuthentication) authenticate(authenticationProviderId) { block() } else block()
}