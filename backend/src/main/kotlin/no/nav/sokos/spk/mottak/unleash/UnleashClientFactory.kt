package no.nav.sokos.spk.mottak.unleash

import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig.UnleashProperties

fun createUnleashClient(
    configuration: PropertiesConfig.Configuration = PropertiesConfig.Configuration(),
    unleashProperties: UnleashProperties = UnleashProperties(),
): Unleash {
    if (PropertiesConfig.isLocal()) {
        return FakeUnleash()
    }
    val config =
        UnleashConfig
            .builder()
            .appName(configuration.naisAppName)
            .instanceId(configuration.naisPodName)
            .unleashAPI(unleashProperties.unleashApi + "/api/")
            .apiKey(unleashProperties.apiKey)
            .synchronousFetchOnInitialisation(true)
            .build()
    return DefaultUnleash(config)
}
