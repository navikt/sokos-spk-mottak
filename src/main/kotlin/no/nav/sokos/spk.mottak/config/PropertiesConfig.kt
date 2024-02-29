package no.nav.sokos.spk.mottak.config

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.io.File

object PropertiesConfig {

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "NAIS_APP_NAME" to "sokos-spk-mottak",
            "NAIS_NAMESPACE" to "okonomi",
            "FILKATALOG_INN" to "inn",
            "FILKATALOG_UT" to "ut",
            "FILKATALOG_FEIL" to "feil"
        )
    )

    private val localDevProperties = ConfigurationMap(
        mapOf(
            "APPLICATION_PROFILE" to Profile.LOCAL.toString(),
            "USE_AUTHENTICATION" to "false",
            "AZURE_APP_CLIENT_ID" to "azure-app-client-id",
            "AZURE_APP_WELL_KNOWN_URL" to "azure-app-well-known-url"
            )
    )

    private val devProperties = ConfigurationMap(mapOf("APPLICATION_PROFILE" to Profile.DEV.toString()))
    private val prodProperties = ConfigurationMap(mapOf("APPLICATION_PROFILE" to Profile.PROD.toString()))

    private val config = when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
        "dev-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding devProperties overriding defaultProperties
        "prod-gcp" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding prodProperties overriding defaultProperties
        else ->
            ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding ConfigurationProperties.fromOptionalFile(
            File("defaults.properties")
        ) overriding localDevProperties overriding defaultProperties
    }

    operator fun get(key: String): String = config[Key(key, stringType)]

    data class Configuration(
        val naisAppName: String = get("NAIS_APP_NAME"),
        val profile: Profile = Profile.valueOf(this["APPLICATION_PROFILE"]),
        val useAuthentication: Boolean = get("USE_AUTHENTICATION").toBoolean(),
        val azureAdConfig: AzureAdConfig = AzureAdConfig()
    )

    data class Db2DatabaseConfig(
        val host: String = get("DATABASE_HOST"),
        val port: String = get("DATABASE_PORT"),
        val name: String = get("DATABASE_NAME"),
        val schema: String = get("DATABASE_SCHEMA"),
        val username: String = get("DATABASE_USERNAME"),
        val password: String = get("DATABASE_PASSWORD"),
    )

    data class FtpConfig(
        val server: String = get("SFTP_SERVER"),
        val username: String = get("SPK_SFTP_USERNAME"),
        val keyPass: String = get("SPK_SFTP_PASSWORD"),
        val privKey: String = get("SFTP_PRIVATE_KEY_FILE_PATH"),
        val hostKey: String = get("SFTP_HOST_KEY_FILE_PATH"),
        val port: Int = get("SFTP_PORT").toInt()
    )

    data class FileConfig(
        val innKatalog: String = get("FILKATALOG_INN"),
        val utKatalog: String = get("FILKATALOG_UT"),
        val feilKatalog: String = get("FILKATALOG_FEIL")
    )

    class AzureAdConfig(
        val clientId: String = this["AZURE_APP_CLIENT_ID"],
        val wellKnownUrl: String = this["AZURE_APP_WELL_KNOWN_URL"]
    )

    enum class Profile {
        LOCAL, DEV, PROD
    }
}

