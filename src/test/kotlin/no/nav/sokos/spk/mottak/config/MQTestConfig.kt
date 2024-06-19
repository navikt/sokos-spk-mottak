package no.nav.sokos.spk.mottak.config

object MQTestConfig {
    fun testMQProperties(): PropertiesConfig.MQProperties =
        PropertiesConfig.MQProperties().copy(
            hostname = "127.0.0.1",
            port = 1414,
            userAuth = false,
        )
}
