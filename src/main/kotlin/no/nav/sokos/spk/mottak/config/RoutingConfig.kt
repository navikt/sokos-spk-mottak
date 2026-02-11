package no.nav.sokos.spk.mottak.config

import io.ktor.server.application.Application
import io.ktor.server.routing.routing

import no.nav.sokos.spk.mottak.api.mottakApi

fun Application.routingConfig(applicationState: ApplicationState) {
    routing {
        internalNaisRoutes(applicationState)
        mottakApi()
    }
}
