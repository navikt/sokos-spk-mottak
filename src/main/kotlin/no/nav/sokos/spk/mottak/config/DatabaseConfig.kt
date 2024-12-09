package no.nav.sokos.spk.mottak.config

import com.ibm.db2.jcc.DB2BaseDataSource
import com.ibm.db2.jcc.DB2SimpleDataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.vault.jdbc.hikaricp.HikariCPVaultUtil
import org.flywaydb.core.Flyway
import org.postgresql.ds.PGSimpleDataSource
import java.time.Duration

private val logger = KotlinLogging.logger {}

object DatabaseConfig {
    private fun db2HikariConfig(): HikariConfig {
        val db2Properties: PropertiesConfig.Db2Properties = PropertiesConfig.Db2Properties()
        return HikariConfig().apply {
            minimumIdle = 1
            maximumPoolSize = 10
            connectionTestQuery = "select 1 from sysibm.sysdummy1"
            dataSource =
                DB2SimpleDataSource().apply {
                    driverType = 4
                    enableNamedParameterMarkers = DB2BaseDataSource.YES
                    databaseName = db2Properties.name
                    serverName = db2Properties.host
                    portNumber = db2Properties.port.toInt()
                    currentSchema = db2Properties.schema
                    connectionTimeout = 1000
                    commandTimeout = 10000
                    user = db2Properties.username
                    setPassword(db2Properties.password)
                }
        }
    }

    private fun postgresHikariConfig(): HikariConfig {
        val postgresProperties: PropertiesConfig.PostgresProperties = PropertiesConfig.PostgresProperties()
        return HikariConfig().apply {
            maximumPoolSize = 10
            minimumIdle = 1
            dataSource =
                PGSimpleDataSource().apply {
                    if (PropertiesConfig.isLocal()) {
                        user = postgresProperties.adminUsername
                        password = postgresProperties.adminPassword
                    }
                    serverNames = arrayOf(postgresProperties.host)
                    databaseName = postgresProperties.databaseName
                    portNumbers = intArrayOf(postgresProperties.port.toInt())
                    connectionTimeout = Duration.ofSeconds(10).toMillis()
                    maxLifetime = Duration.ofMinutes(30).toMillis()
                    initializationFailTimeout = Duration.ofMinutes(30).toMillis()
                }
        }
    }

    fun db2DataSource(): HikariDataSource = HikariDataSource(db2HikariConfig())

    fun postgresDataSource(
        hikariConfig: HikariConfig = postgresHikariConfig(),
        role: String = PropertiesConfig.PostgresProperties().user,
    ): HikariDataSource =
        when {
            PropertiesConfig.isLocal() -> HikariDataSource(hikariConfig)
            else ->
                HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                    hikariConfig,
                    PropertiesConfig.PostgresProperties().vaultMountPath,
                    role,
                )
        }

    fun postgresMigrate(dataSource: HikariDataSource = postgresDataSource(role = PropertiesConfig.PostgresProperties().adminUser)) {
        Flyway
            .configure()
            .dataSource(dataSource)
            .initSql("""SET ROLE "${PropertiesConfig.PostgresProperties().adminUser}"""")
            .lockRetryCount(-1)
            .validateMigrationNaming(true)
            .load()
            .migrate()
            .migrationsExecuted
        logger.info { "Migration finished" }
    }
}

fun <A> HikariDataSource.transaction(operation: (TransactionalSession) -> A): A =
    using(sessionOf(this, returnGeneratedKey = true)) { session ->
        session.transaction { tx ->
            operation(tx)
        }
    }
