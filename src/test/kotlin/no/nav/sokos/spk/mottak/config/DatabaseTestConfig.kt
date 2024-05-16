package no.nav.sokos.spk.mottak.config

import com.zaxxer.hikari.HikariConfig
import org.postgresql.ds.PGSimpleDataSource
import java.time.Duration

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
            username = PropertiesConfig.PostgresConfig().adminUser
            password = "postgres"
            dataSource =
                PGSimpleDataSource().apply {
                    serverNames = arrayOf(host)
                    databaseName = PropertiesConfig.PostgresConfig().databaseName
                    portNumbers = intArrayOf(PropertiesConfig.PostgresConfig().port.toInt())
                    connectionTimeout = Duration.ofSeconds(10).toMillis()
                    maxLifetime = Duration.ofMinutes(30).toMillis()
                    initializationFailTimeout = Duration.ofMinutes(30).toMillis()
                }
        }
}
