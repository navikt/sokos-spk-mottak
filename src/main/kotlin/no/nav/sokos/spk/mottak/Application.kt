package no.nav.sokos.spk.mottak

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.JobTaskConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.commonConfig
import no.nav.sokos.spk.mottak.config.configureSecurity
import no.nav.sokos.spk.mottak.config.routingConfig
import no.nav.sokos.spk.mottak.metrics.Metrics
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

fun main() {
    val applicationState = ApplicationState()
    val applicationConfiguration = PropertiesConfig.Configuration()
    DatabaseConfig.postgresMigrate()
    JobTaskConfig.scheduler().start()

    HttpServer(applicationState, applicationConfiguration).start()
}

class HttpServer(
    private val applicationState: ApplicationState,
    private val applicationConfiguration: PropertiesConfig.Configuration,
    port: Int = 8080,
) {
    private val embeddedServer =
        embeddedServer(Netty, port, module = {
            applicationModule(applicationConfiguration, applicationState)
        })

    fun start() {
        applicationState.running = true
        embeddedServer.start(wait = true)
    }

    private fun stop() {
        applicationState.running = false
        embeddedServer.stop(5, 5, TimeUnit.SECONDS)
    }
}

class ApplicationState(
    alive: Boolean = true,
    ready: Boolean = false,
) {
    var initialized: Boolean by Delegates.observable(alive) { _, _, newValue ->
        if (!newValue) Metrics.appStateReadyFalse.inc()
    }
    var running: Boolean by Delegates.observable(ready) { _, _, newValue ->
        if (!newValue) Metrics.appStateRunningFalse.inc()
    }
}

fun Application.applicationModule(
    applicationConfiguration: PropertiesConfig.Configuration,
    applicationState: ApplicationState,
) {
    commonConfig()
    configureSecurity(applicationConfiguration.azureAdConfig, applicationConfiguration.useAuthentication)
    routingConfig(applicationState, applicationConfiguration.useAuthentication)
}
