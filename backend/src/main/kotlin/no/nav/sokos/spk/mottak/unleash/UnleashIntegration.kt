package no.nav.sokos.spk.mottak.unleash

import io.getunleash.Unleash

import no.nav.sokos.spk.mottak.config.PropertiesConfig.UnleashProperties

class UnleashIntegration(
    private val unleashClient: Unleash,
    private val enabledByDefault: Boolean = UnleashProperties().enabledByDefault,
) {
    private enum class ToggleNames(
        val key: String,
    ) {
        SCHEDULER_ENABLED("sokos-spk-mottak.schedulerEnabled"),
        LISTENER_ENABLED("sokos-spk-mottak.mqListenerEnabled"),
        DB_HISTORY_CLEANUP_ENABLED("sokos-spk-mottak.dbHistoryCleanupEnabled"),
    }

    // Kill switcher:
    fun isSchedulerEnabled(): Boolean = isEnabled(ToggleNames.SCHEDULER_ENABLED.key)

    fun isListenerEnabled(): Boolean = isEnabled(ToggleNames.LISTENER_ENABLED.key)

    fun isHistoryCleanupEnabled(): Boolean = isEnabled(ToggleNames.DB_HISTORY_CLEANUP_ENABLED.key)

    fun isEnabled(toggleName: String): Boolean = unleashClient.isEnabled(toggleName, enabledByDefault)

    companion object {
        val instance: UnleashIntegration by lazy { UnleashIntegration(createUnleashClient()) }
    }
}
