package no.nav.sokos.spk.mottak

import io.ktor.server.application.Application
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty

import no.nav.sokos.spk.mottak.config.ApplicationState
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.JobTaskConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.applicationLifecycleConfig
import no.nav.sokos.spk.mottak.config.commonConfig
import no.nav.sokos.spk.mottak.config.routingConfig
import no.nav.sokos.spk.mottak.config.securityConfig
import no.nav.sokos.spk.mottak.mq.AvregningService
import no.nav.sokos.spk.mottak.mq.JmsListenerService

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module).start(true)
}

private fun Application.module() {
    val useAuthentication = PropertiesConfig.Configuration().useAuthentication
    val applicationState = ApplicationState()
    commonConfig()
    applicationLifecycleConfig(applicationState)
    securityConfig(useAuthentication)
    routingConfig(useAuthentication, applicationState)

    DatabaseConfig.postgresMigrate()
    if (PropertiesConfig.MQProperties().mqListenerEnabled) {
        JmsListenerService().start()
    }

    if (PropertiesConfig.MQProperties().avregningListenerEnabled) {
        AvregningService().start()
    }

    if (PropertiesConfig.SchedulerProperties().enabled) {
        JobTaskConfig.scheduler().start()
    }
}
