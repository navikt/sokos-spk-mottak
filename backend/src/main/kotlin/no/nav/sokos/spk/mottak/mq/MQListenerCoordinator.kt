package no.nav.sokos.spk.mottak.mq

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal data class ListenerRegistration(
    val name: String,
    val enabledByEnvironment: Boolean,
    val create: () -> ManagedMqListener,
)

internal class MQListenerCoordinator(
    private val isEnabled: () -> Boolean,
    private val registrations: List<ListenerRegistration>,
) : AutoCloseable {
    private val active = linkedMapOf<String, ManagedMqListener>()
    private var shuttingDown = false

    @Synchronized
    fun reconcile() {
        if (shuttingDown) return

        val enabled = isEnabled()

        registrations.forEach { registration ->
            val listener = active[registration.name]
            if (enabled && registration.enabledByEnvironment && listener == null) {
                start(registration)
            } else if ((!enabled || !registration.enabledByEnvironment) && listener != null) {
                stop(registration.name, listener)
            }
        }
    }

    private fun start(registration: ListenerRegistration) {
        val listener =
            try {
                registration.create()
            } catch (exception: Exception) {
                logger.error(exception) { "Kunne ikke opprette MQ-listener ${registration.name}" }
                return
            }

        try {
            listener.start()
            active[registration.name] = listener
            logger.info { "Startet MQ-listener ${registration.name}" }
        } catch (exception: Exception) {
            logger.error(exception) { "Kunne ikke starte MQ-listener ${registration.name}" }
            try {
                listener.close()
            } catch (closeException: Exception) {
                logger.error(closeException) { "Kunne ikke lukke MQ-listener ${registration.name} etter startfeil" }
            }
        }
    }

    private fun stop(
        name: String,
        listener: ManagedMqListener,
    ) {
        try {
            listener.close()
            active.remove(name)
            logger.info { "Lukket MQ-listener $name" }
        } catch (exception: Exception) {
            logger.error(exception) { "Kunne ikke lukke MQ-listener $name" }
        }
    }

    @Synchronized
    override fun close() {
        shuttingDown = true
        active.toMap().forEach { (name, listener) -> stop(name, listener) }
    }
}
