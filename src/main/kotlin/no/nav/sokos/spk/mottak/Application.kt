package no.nav.sokos.spk.mottak

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import no.nav.sokos.spk.mottak.config.ApplicationState
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.JobTaskConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.applicationLifecycleConfig
import no.nav.sokos.spk.mottak.config.commonConfig
import no.nav.sokos.spk.mottak.config.routingConfig
import no.nav.sokos.spk.mottak.config.securityConfig
import java.util.concurrent.TimeUnit

fun main() {
    HttpServer(8080).start()
}

private fun Application.serverModule() {
    val useAuthentication = PropertiesConfig.Configuration().useAuthentication
    val applicationState = ApplicationState()
    DatabaseConfig.postgresMigrate()
    JobTaskConfig.scheduler().start()

    commonConfig()
    applicationLifecycleConfig(applicationState)
    securityConfig(useAuthentication)
    routingConfig(useAuthentication, applicationState)
}

private class HttpServer(
    port: Int,
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
            serverModule()
        })

    fun start() {
        embeddedServer.start(wait = true)
    }

    private fun stop() {
        embeddedServer.stop(5, 5, TimeUnit.SECONDS)
    }
}
