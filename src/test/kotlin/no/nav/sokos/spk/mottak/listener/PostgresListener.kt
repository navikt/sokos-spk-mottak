package no.nav.sokos.spk.mottak.listener

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.TestListener
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.DatabaseTestConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

object PostgresListener : TestListener {
    private val container =
        GenericContainer("postgres:16-alpine")
            .withExposedPorts(5432)
            .withEnv("POSTGRES_DB", PropertiesConfig.PostgresProperties().databaseName)
            .withEnv("POSTGRES_USER", PropertiesConfig.PostgresProperties().adminUser)
            .withEnv("POSTGRES_PASSWORD", "postgres")
            .withCreateContainerCmdModifier { cmd ->
                cmd.hostConfig!!.withPortBindings(PortBinding(Ports.Binding.bindPort(5432), ExposedPort(5432)))
            }
            .waitingFor(Wait.forLogMessage(".*ready to accept connections*\\n", 1))

    val dataSource: HikariDataSource by lazy {
        DatabaseConfig.postgresDataSource(DatabaseTestConfig.hikariPostgresConfig(container.host))
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
