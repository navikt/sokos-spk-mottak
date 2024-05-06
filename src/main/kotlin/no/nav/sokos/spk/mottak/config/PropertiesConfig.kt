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
                // Azure
                "AZURE_APP_CLIENT_ID" to "",
                "AZURE_APP_WELL_KNOWN_URL" to "",
                "AZURE_APP_TENANT_ID" to "",
                "AZURE_APP_CLIENT_SECRET" to "",
                // DB2
                "DATABASE_HOST" to "",
                "DATABASE_PORT" to "",
                "DATABASE_NAME" to "",
                "DATABASE_SCHEMA" to "",
                "DATABASE_USERNAME" to "",
                "DATABASE_PASSWORD" to "",
                // POSTGRES
                "POSTGRES_USER_USERNAME" to "",
                "POSTGRES_USER_PASSWORD" to "",
                "POSTGRES_ADMIN_USERNAME" to "",
                "POSTGRES_ADMIN_PASSWORD" to "",
                "VAULT_MOUNTPATH" to "",
                // SFTP
                "SFTP_SERVER" to "",
                "SPK_SFTP_USERNAME" to "",
                "SFTP_PRIVATE_KEY_FILE_PATH" to "",
                "SPK_SFTP_PASSWORD" to "",
                "SFTP_PORT" to "",
                // Pensjon Representasjon
                "PENSJON_REPRESENTASJON_URL" to "",
                "PENSJON_REPRESENTASJON_SCOPE" to "",
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

    fun getOrNull(key: String): String? = config.getOrNull(Key(key, stringType))

    data class Configuration(
        val naisAppName: String = get("NAIS_APP_NAME"),
        val profile: Profile = Profile.valueOf(get("APPLICATION_PROFILE")),
        val useAuthentication: Boolean = get("USE_AUTHENTICATION").toBoolean(),
        val azureAdConfig: AzureAdConfig = AzureAdConfig(),
    )

    data class Db2DatabaseConfig(
        val host: String = get("DATABASE_HOST"),
        val port: String = get("DATABASE_PORT"),
        val name: String = get("DATABASE_NAME"),
        val schema: String = get("DATABASE_SCHEMA"),
        val username: String = get("DATABASE_USERNAME"),
        val password: String = get("DATABASE_PASSWORD"),
    )

    data class SftpConfig(
        val host: String = get("SFTP_SERVER"),
        val username: String = get("SPK_SFTP_USERNAME"),
        val privateKey: String = get("SFTP_PRIVATE_KEY_FILE_PATH"),
        val privateKeyPassword: String = get("SPK_SFTP_PASSWORD"),
        val port: Int = get("SFTP_PORT").toInt(),
    )

    data class AzureAdConfig(
        val clientId: String = get("AZURE_APP_CLIENT_ID"),
        val wellKnownUrl: String = get("AZURE_APP_WELL_KNOWN_URL"),
        val tenantId: String = get("AZURE_APP_TENANT_ID"),
        val clientSecret: String = get("AZURE_APP_CLIENT_SECRET"),
    )

    data class PensjonFullmaktConfig(
        val fullmaktUrl: String = get("PENSJON_REPRESENTASJON_URL"),
        val fullmaktScope: String = get("PENSJON_REPRESENTASJON_SCOPE"),
    )

    data class PostgresConfig(
        val host: String = get("POSTGRES_HOST"),
        val port: String = get("POSTGRES_PORT"),
        val databaseName: String = get("POSTGRES_NAME"),
        val username: String? = getOrNull("POSTGRES_USER_USERNAME"),
        val password: String? = getOrNull("POSTGRES_USER_PASSWORD"),
        val adminUsername: String? = getOrNull("POSTGRES_ADMIN_USERNAME"),
        val adminPassword: String? = getOrNull("POSTGRES_ADMIN_PASSWORD"),
        val vaultMountPath: String = get("VAULT_MOUNTPATH"),
    ) {
        val adminUser = "${get("POSTGRES_NAME")}-admin"
        val user = "${get("POSTGRES_NAME")}-user"
    }

    enum class Profile {
        LOCAL,
        DEV,
        PROD,
    }
}

fun PropertiesConfig.isLocal() = PropertiesConfig.Configuration().profile == PropertiesConfig.Profile.LOCAL
