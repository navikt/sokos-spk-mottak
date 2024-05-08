package no.nav.sokos.spk.mottak.config

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import java.io.File

object PropertiesConfig {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "NAIS_APP_NAME" to "sokos-spk-mottak",
                "NAIS_NAMESPACE" to "okonomi",
                "READ_AND_PARSEFILE_CRON_PATTERN" to "0 0 10 * * *",
                "VALIDATE_TRANSAKSJON_CRON_PATTERN" to "0 30 * * * *",
            ),
        )

    private val localDevProperties =
        ConfigurationMap(
            mapOf(
                "APPLICATION_PROFILE" to Profile.LOCAL.toString(),
                "USE_AUTHENTICATION" to "false",
                // Postgres
                "POSTGRES_HOST" to "dev-pg.intern.nav.no",
                "POSTGRES_PORT" to "5432",
                "POSTGRES_NAME" to "sokos-spk-mottak",
            ),
        )

    private val devProperties = ConfigurationMap(mapOf("APPLICATION_PROFILE" to Profile.DEV.toString()))
    private val prodProperties = ConfigurationMap(mapOf("APPLICATION_PROFILE" to Profile.PROD.toString()))

    private val config =
        when (System.getenv("NAIS_CLUSTER_NAME") ?: System.getProperty("NAIS_CLUSTER_NAME")) {
            "dev-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding devProperties overriding defaultProperties
            "prod-fss" -> ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding prodProperties overriding defaultProperties
            else ->
                ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding
                    ConfigurationProperties.fromOptionalFile(
                        File("defaults.properties"),
                    ) overriding localDevProperties overriding defaultProperties
        }

    operator fun get(key: String): String = config[Key(key, stringType)]

    fun getOrEmpty(key: String): String = config.getOrElse(Key(key, stringType), "")

    data class Configuration(
        val naisAppName: String = get("NAIS_APP_NAME"),
        val profile: Profile = Profile.valueOf(get("APPLICATION_PROFILE")),
        val useAuthentication: Boolean = get("USE_AUTHENTICATION").toBoolean(),
        val azureAdConfig: AzureAdConfig = AzureAdConfig(),
    )

    data class Db2DatabaseConfig(
        val host: String = getOrEmpty("DATABASE_HOST"),
        val port: String = getOrEmpty("DATABASE_PORT"),
        val name: String = getOrEmpty("DATABASE_NAME"),
        val schema: String = getOrEmpty("DATABASE_SCHEMA"),
        val username: String = getOrEmpty("DATABASE_USERNAME"),
        val password: String = getOrEmpty("DATABASE_PASSWORD"),
    )

    data class SftpConfig(
        val host: String = getOrEmpty("SFTP_SERVER"),
        val username: String = getOrEmpty("SPK_SFTP_USERNAME"),
        val privateKey: String = getOrEmpty("SFTP_PRIVATE_KEY_FILE_PATH"),
        val privateKeyPassword: String = getOrEmpty("SPK_SFTP_PASSWORD"),
        val port: Int = getOrEmpty("SFTP_PORT").toInt(),
    )

    data class AzureAdConfig(
        val clientId: String = getOrEmpty("AZURE_APP_CLIENT_ID"),
        val wellKnownUrl: String = getOrEmpty("AZURE_APP_WELL_KNOWN_URL"),
        val tenantId: String = getOrEmpty("AZURE_APP_TENANT_ID"),
        val clientSecret: String = getOrEmpty("AZURE_APP_CLIENT_SECRET"),
    )

    data class PensjonFullmaktConfig(
        val fullmaktUrl: String = getOrEmpty("PENSJON_REPRESENTASJON_URL"),
        val fullmaktScope: String = getOrEmpty("PENSJON_REPRESENTASJON_SCOPE"),
    )

    data class PostgresConfig(
        val host: String = get("POSTGRES_HOST"),
        val port: String = get("POSTGRES_PORT"),
        val databaseName: String = get("POSTGRES_NAME"),
        val username: String? = getOrEmpty("POSTGRES_USER_USERNAME"),
        val password: String? = getOrEmpty("POSTGRES_USER_PASSWORD"),
        val adminUsername: String? = getOrEmpty("POSTGRES_ADMIN_USERNAME"),
        val adminPassword: String? = getOrEmpty("POSTGRES_ADMIN_PASSWORD"),
        val vaultMountPath: String = getOrEmpty("VAULT_MOUNTPATH"),
    ) {
        val adminUser = "${get("POSTGRES_NAME")}-admin"
        val user = "${get("POSTGRES_NAME")}-user"
    }

    data class SchedulerConfig(
        val readAndParseFileCronPattern: String = getOrEmpty("READ_AND_PARSEFILE_CRON_PATTERN"),
        val validateTransaksjonCronPattern: String = getOrEmpty("VALIDATE_TRANSAKSJON_CRON_PATTERN"),
    )

    enum class Profile {
        LOCAL,
        DEV,
        PROD,
    }

    fun isLocal() = Configuration().profile == Profile.LOCAL
}
