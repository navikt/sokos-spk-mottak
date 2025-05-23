package no.nav.sokos.spk.mottak.config

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry

val openTelemetry = GlobalOpenTelemetry.get()

fun Application.setupServerOpentelemetry() {
    install(KtorServerTelemetry) {
        setOpenTelemetry(openTelemetry)
    }
}
