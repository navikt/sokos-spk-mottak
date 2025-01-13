package no.nav.sokos.spk.mottak.config

import java.time.Duration

import com.zaxxer.hikari.HikariConfig
import org.postgresql.ds.PGSimpleDataSource

object DatabaseTestConfig {
    fun hikariConfig() =
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:test_mottak;MODE=DB2;DB_CLOSE_DELAY=-1;"
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 10
            validate()
        }

    fun hikariPostgresConfig(host: String) =
        HikariConfig().apply {
            poolName = "HikariPool-SOKOS-SPK-MOTTAK-POSTGRES"
            maximumPoolSize = 5
            minimumIdle = 1
            username = PropertiesConfig.PostgresProperties().adminUser
            password = "postgres"
            dataSource =
                PGSimpleDataSource().apply {
                    serverNames = arrayOf(host)
                    databaseName = PropertiesConfig.PostgresProperties().databaseName
                    portNumbers = intArrayOf(PropertiesConfig.PostgresProperties().port.toInt())
                    connectionTimeout = Duration.ofSeconds(10).toMillis()
                    maxLifetime = Duration.ofMinutes(30).toMillis()
                    initializationFailTimeout = Duration.ofMinutes(30).toMillis()
                }
        }
}
