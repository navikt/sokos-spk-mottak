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
        val db2DatabaseConfig: PropertiesConfig.Db2DatabaseConfig = PropertiesConfig.Db2DatabaseConfig()
        return HikariConfig().apply {
            minimumIdle = 1
            maximumPoolSize = 10
            poolName = "HikariPool-SOKOS-SPK-MOTTAK"
            connectionTestQuery = "select 1 from sysibm.sysdummy1"
            dataSource =
                DB2SimpleDataSource().apply {
                    driverType = 4
                    enableNamedParameterMarkers = DB2BaseDataSource.YES
                    databaseName = db2DatabaseConfig.name
                    serverName = db2DatabaseConfig.host
                    portNumber = db2DatabaseConfig.port.toInt()
                    currentSchema = db2DatabaseConfig.schema
                    connectionTimeout = 1000
                    commandTimeout = 10000
                    user = db2DatabaseConfig.username
                    setPassword(db2DatabaseConfig.password)
                }
        }
    }

    private fun postgresHikariConfig(): HikariConfig {
        val postgresConfig: PropertiesConfig.PostgresConfig = PropertiesConfig.PostgresConfig()
        return HikariConfig().apply {
            poolName = "HikariPool-SOKOS-SPK-MOTTAK-POSTGRES"
            maximumPoolSize = 5
            minimumIdle = 1
            dataSource =
                PGSimpleDataSource().apply {
                    if (PropertiesConfig.isLocal()) {
                        user = postgresConfig.adminUsername
                        password = postgresConfig.adminPassword
                    }
                    serverNames = arrayOf(postgresConfig.host)
                    databaseName = postgresConfig.databaseName
                    portNumbers = intArrayOf(postgresConfig.port.toInt())
                    connectionTimeout = Duration.ofSeconds(10).toMillis()
                    maxLifetime = Duration.ofMinutes(30).toMillis()
                    initializationFailTimeout = Duration.ofMinutes(30).toMillis()
                }
        }
    }

    fun db2DataSource(): HikariDataSource = HikariDataSource(db2HikariConfig())

    fun postgresDataSource(
        hikariConfig: HikariConfig = postgresHikariConfig(),
        role: String = PropertiesConfig.PostgresConfig().user,
    ): HikariDataSource {
        return when {
            PropertiesConfig.isLocal() -> HikariDataSource(hikariConfig)
            else ->
                HikariCPVaultUtil.createHikariDataSourceWithVaultIntegration(
                    hikariConfig,
                    PropertiesConfig.PostgresConfig().vaultMountPath,
                    role,
                )
        }
    }

    fun postgresMigrate(dataSource: HikariDataSource = postgresDataSource(role = PropertiesConfig.PostgresConfig().adminUser)) {
        Flyway.configure()
            .dataSource(dataSource)
            .initSql("""SET ROLE "${PropertiesConfig.PostgresConfig().adminUser}"""")
            .lockRetryCount(-1)
            .load()
            .migrate()
            .migrationsExecuted
        logger.info { "Migration finished" }
    }
}

fun <A> HikariDataSource.transaction(operation: (TransactionalSession) -> A): A {
    return using(sessionOf(this, returnGeneratedKey = true)) { session ->
        session.transaction { tx ->
            operation(tx)
        }
    }
}
