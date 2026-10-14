package no.nav.sokos.spk.mottak.config

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStopped

import no.nav.sokos.spk.mottak.mq.AvregningListenerService
import no.nav.sokos.spk.mottak.mq.ListenerRegistration
import no.nav.sokos.spk.mottak.mq.MQListenerCoordinator
import no.nav.sokos.spk.mottak.mq.MQListenerPollingJob
import no.nav.sokos.spk.mottak.mq.TrekkListenerService
import no.nav.sokos.spk.mottak.mq.UtbetalingListenerService
import no.nav.sokos.spk.mottak.unleash.UnleashIntegration

fun Application.mqListenerSetup() {
    val mqProperties = PropertiesConfig.MQProperties()
    // BOQ-producerne lever like lenge som appen, mens listenerne lages på nytt når Unleash-flagget slås på.
    val utbetalingBoqProducer = lazy { UtbetalingListenerService.boqProducer() }
    val trekkBoqProducer = lazy { TrekkListenerService.boqProducer() }
    val avregningBoqProducer = lazy { AvregningListenerService.boqProducer() }

    val coordinator =
        MQListenerCoordinator(
            isEnabled = { UnleashIntegration.instance.isListenerEnabled() },
            registrations =
                listOf(
                    ListenerRegistration("utbetaling", mqProperties.mqListenerEnabled) { UtbetalingListenerService(producer = utbetalingBoqProducer.value) },
                    ListenerRegistration("trekk", mqProperties.mqListenerEnabled) { TrekkListenerService(producer = trekkBoqProducer.value) },
                    ListenerRegistration("avregning", mqProperties.avregningListenerEnabled) { AvregningListenerService(producer = avregningBoqProducer.value) },
                ),
        )
    val pollingJob = MQListenerPollingJob(coordinator::reconcile, coordinator::close)

    monitor.subscribe(ApplicationStopped) {
        pollingJob.close()
        listOf(utbetalingBoqProducer, trekkBoqProducer, avregningBoqProducer)
            .filter { it.isInitialized() }
            .forEach { it.value.close() }
    }
    pollingJob.start()
}
