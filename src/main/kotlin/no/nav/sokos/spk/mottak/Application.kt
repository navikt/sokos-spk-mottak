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
import java.util.concurrent.TimeUnit

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
    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                this.stop()
            },
        )
    }

    private val embeddedServer =
        embeddedServer(Netty, port, module = {
            applicationModule(applicationConfiguration, applicationState)
            applicationState.initialized = true
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
    var running: Boolean = false,
    var initialized: Boolean = false,
)

fun Application.applicationModule(
    applicationConfiguration: PropertiesConfig.Configuration,
    applicationState: ApplicationState,
) {
    commonConfig()
    configureSecurity(applicationConfiguration.azureAdProperties, applicationConfiguration.useAuthentication)
    routingConfig(applicationState, applicationConfiguration.useAuthentication)
}
