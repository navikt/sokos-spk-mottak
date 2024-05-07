package no.nav.sokos.spk.mottak.listener

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import java.time.Duration

object PostgresListener : TestListener {
    private val container =
        GenericContainer("postgres:15-alpine")
            .withExposedPorts(5432)
            .withEnv("POSTGRES_DB", PropertiesConfig.PostgresConfig().databaseName)
            .withEnv("POSTGRES_USER", PropertiesConfig.PostgresConfig().adminUser)
            .withEnv("POSTGRES_PASSWORD", "postgres")
            .withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig!!.withPortBindings(PortBinding(Ports.Binding.bindPort(5432), ExposedPort(5432)))
            }
            .waitingFor(Wait.forLogMessage(".*ready to accept connections*\\n", 1))

    private val hikariConfig =
        HikariConfig().apply {
            poolName = "HikariPool-SOKOS-SPK-MOTTAK-POSTGRES"
            maximumPoolSize = 5
            minimumIdle = 1
            username = PropertiesConfig.PostgresConfig().adminUser
            password = "postgres"
            dataSource =
                PGSimpleDataSource().apply {
                    serverNames = arrayOf(container.host)
                    databaseName = PropertiesConfig.PostgresConfig().databaseName
                    portNumbers = intArrayOf(PropertiesConfig.PostgresConfig().port.toInt())
                    connectionTimeout = Duration.ofSeconds(10).toMillis()
                    maxLifetime = Duration.ofMinutes(30).toMillis()
                    initializationFailTimeout = Duration.ofMinutes(30).toMillis()
                }
        }

    val dataSource: HikariDataSource by lazy {
        DatabaseConfig.postgresDataSource(hikariConfig)
    }

    override suspend fun beforeTest(testCase: TestCase) {
        container.start()
        DatabaseConfig.postgresMigrate(dataSource)
    }

    override suspend fun afterTest(
        testCase: TestCase,
        result: TestResult,
    ) {
        container.stop()
    }
}
