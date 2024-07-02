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
                "USE_AUTHENTICATION" to "true",
                "READ_AND_PARSEFILE_CRON_PATTERN" to "0 10 * * * *",
                "VALIDATE_TRANSAKSJON_CRON_PATTERN" to "0 * * * * *",
                "SEND_UTBETALING_TRANSAKSJON_TIL_OPPDRAG_CRON_PATTERN" to "* * * * * *",
                "SEND_TREKK_TRANSAKSJON_TIL_OPPDRAG_CRON_PATTERN" to "* * * * * *",
            ),
        )

    private val localDevProperties =
        ConfigurationMap(
            mapOf(
                "APPLICATION_PROFILE" to Profile.LOCAL.toString(),
                "USE_AUTHENTICATION" to "false",
                "POSTGRES_HOST" to "dev-pg.intern.nav.no",
                "POSTGRES_PORT" to "5432",
                "POSTGRES_NAME" to "sokos-spk-mottak",
                "MQ_HOSTNAME" to "10.53.17.118",
                "MQ_PORT" to "1413",
                "MQ_QUEUE_MANAGER_NAME" to "MQLS02",
                "MQ_CHANNEL_NAME" to "Q1_MOT",
                "MQ_UTBETALING_QUEUE_NAME" to "QA.Q1_231.OB04_OPPDRAG_MOT_XML",
                "MQ_UTBETALING_REPLY_QUEUE_NAME" to "QA.Q1_MOT.UTBET_REQUEST_QUE_MOT_BATCH_REPLY",
                "MQ_TREKK_QUEUE_NAME" to "QA.DY_231.OB04_INNRAPPORTERING_TREKK",
                "MQ_TREKK_REPLY_QUEUE_NAME" to "QA.Q1_MOT.TREKK_REQUEST_QUE_MOT_BATCH_REPLY",
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
        val useAuthentication: Boolean = getOrEmpty("USE_AUTHENTICATION").toBoolean(),
        val azureAdProperties: AzureAdProperties = AzureAdProperties(),
    )

    data class Db2Properties(
        val host: String = getOrEmpty("DATABASE_HOST"),
        val port: String = getOrEmpty("DATABASE_PORT"),
        val name: String = getOrEmpty("DATABASE_NAME"),
        val schema: String = getOrEmpty("DATABASE_SCHEMA"),
        val username: String = getOrEmpty("DATABASE_USERNAME"),
        val password: String = getOrEmpty("DATABASE_PASSWORD"),
    )

    data class SftpProperties(
        val host: String = getOrEmpty("SFTP_SERVER"),
        val username: String = getOrEmpty("SPK_SFTP_USERNAME"),
        val privateKey: String = getOrEmpty("SFTP_PRIVATE_KEY_FILE_PATH"),
        val privateKeyPassword: String = getOrEmpty("SPK_SFTP_PASSWORD"),
        val port: Int = getOrEmpty("SFTP_PORT").toInt(),
    )

    data class AzureAdProperties(
        val clientId: String = getOrEmpty("AZURE_APP_CLIENT_ID"),
        val wellKnownUrl: String = getOrEmpty("AZURE_APP_WELL_KNOWN_URL"),
        val tenantId: String = getOrEmpty("AZURE_APP_TENANT_ID"),
        val clientSecret: String = getOrEmpty("AZURE_APP_CLIENT_SECRET"),
    )

    data class PostgresProperties(
        val host: String = get("POSTGRES_HOST"),
        val port: String = get("POSTGRES_PORT"),
        val databaseName: String = get("POSTGRES_NAME"),
        val username: String = getOrEmpty("POSTGRES_USER_USERNAME"),
        val password: String = getOrEmpty("POSTGRES_USER_PASSWORD"),
        val adminUsername: String = getOrEmpty("POSTGRES_ADMIN_USERNAME"),
        val adminPassword: String = getOrEmpty("POSTGRES_ADMIN_PASSWORD"),
        val vaultMountPath: String = getOrEmpty("VAULT_MOUNTPATH"),
    ) {
        val adminUser = "${get("POSTGRES_NAME")}-admin"
        val user = "${get("POSTGRES_NAME")}-user"
    }

    data class SchedulerProperties(
        val readAndParseFileCronPattern: String = getOrEmpty("READ_AND_PARSEFILE_CRON_PATTERN"),
        val validateTransaksjonCronPattern: String = getOrEmpty("VALIDATE_TRANSAKSJON_CRON_PATTERN"),
        val sendUtbetalingTransaksjonTilOppdragCronPattern: String = getOrEmpty("SEND_UTBETALING_TRANSAKSJON_TIL_OPPDRAG_CRON_PATTERN"),
        val sendTrekkTransaksjonTilOppdragCronPattern: String = getOrEmpty("SEND_TREKK_TRANSAKSJON_TIL_OPPDRAG_CRON_PATTERN"),
    )

    data class MQProperties(
        val hostname: String = get("MQ_HOSTNAME"),
        val port: Int = get("MQ_PORT").toInt(),
        val mqQueueManagerName: String = get("MQ_QUEUE_MANAGER_NAME"),
        val mqChannelName: String = getOrEmpty("MQ_CHANNEL_NAME"),
        val serviceUsername: String = getOrEmpty("MQ_SERVICE_USERNAME"),
        val servicePassword: String = getOrEmpty("MQ_SERVICE_PASSWORD"),
        val trekkQueueName: String = getOrEmpty("MQ_TREKK_QUEUE_NAME"),
        val trekkReplyQueueName: String = getOrEmpty("MQ_TREKK_REPLY_QUEUE_NAME"),
        val utbetalingQueueName: String = getOrEmpty("MQ_UTBETALING_QUEUE_NAME"),
        val utbetalingReplyQueueName: String = getOrEmpty("MQ_UTBETALING_REPLY_QUEUE_NAME"),
        val userAuth: Boolean = true,
    )

    enum class Profile {
        LOCAL,
        DEV,
        PROD,
    }

    fun isLocal() = Configuration().profile == Profile.LOCAL
}
