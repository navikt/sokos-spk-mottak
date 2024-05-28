package no.nav.sokos.spk.mottak

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.JobTaskConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.commonConfig
import no.nav.sokos.spk.mottak.config.configureLifecycleConfig
import no.nav.sokos.spk.mottak.config.configureSecurity
import no.nav.sokos.spk.mottak.config.routingConfig
import java.util.concurrent.TimeUnit

fun main() {
    HttpServer().start()
}

fun Application.serverModule() {
    val applicationState = ApplicationState()
    val applicationConfiguration = PropertiesConfig.Configuration()
    DatabaseConfig.postgresMigrate()
    JobTaskConfig.scheduler().start()

    commonConfig()
    configureLifecycleConfig(applicationState)
    configureSecurity(applicationConfiguration.azureAdProperties, applicationConfiguration.useAuthentication)
    routingConfig(applicationState, applicationConfiguration.useAuthentication)
}

private class HttpServer(port: Int = 8080) {
    init {
        Runtime.getRuntime().addShutdownHook(
            Thread {
                this.stop()
            },
        )
    }

    private val embeddedServer =
        embeddedServer(Netty, port, module = {
            serverModule()
        })

    fun start() {
        embeddedServer.start(wait = true)
    }

    private fun stop() {
        embeddedServer.stop(5, 5, TimeUnit.SECONDS)
    }
}

class ApplicationState(
    var ready: Boolean = true,
    var alive: Boolean = true,
)
