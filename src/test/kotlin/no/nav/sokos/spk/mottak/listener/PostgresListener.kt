package no.nav.sokos.spk.mottak.listener

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.mockk.spyk
import kotliquery.queryOf
import org.flywaydb.core.Flyway
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait

import no.nav.sokos.spk.mottak.config.DatabaseTestConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.repository.ScheduledTaskRepository
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

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
        HikariDataSource(DatabaseTestConfig.hikariPostgresConfig(container.host))
    }

    val scheduledTaskRepository: ScheduledTaskRepository by lazy {
        spyk(ScheduledTaskRepository(dataSource))
    }

    override suspend fun beforeSpec(spec: Spec) {
        container.start()
        Flyway
            .configure()
            .dataSource(dataSource)
            .initSql("""SET ROLE "${PropertiesConfig.PostgresProperties().adminUser}"""")
            .lockRetryCount(-1)
            .validateMigrationNaming(true)
            .load()
            .migrate()
            .migrationsExecuted
    }

    override suspend fun afterEach(
        testCase: TestCase,
        result: TestResult,
    ) {
        dataSource.transaction { session ->
            session.update(queryOf("DELETE FROM SCHEDULED_TASKS_HISTORY"))
            session.update(queryOf("DELETE FROM SCHEDULED_TASKS"))
        }
    }

    override suspend fun afterSpec(spec: Spec) {
        container.stop()
    }
}
