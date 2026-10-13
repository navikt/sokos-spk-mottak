package no.nav.sokos.spk.mottak.listener

import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.engine.test.TestResult
import io.mockk.spyk
import kotliquery.queryOf
import org.flywaydb.core.Flyway
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName

import no.nav.sokos.spk.mottak.config.DatabaseTestConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.repository.ScheduledTaskRepository
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

object PostgresListener : TestListener {
    private const val DOCKER_IMAGE_NAME = "postgres:16"
    private val postgresProperties = PropertiesConfig.PostgresProperties()
    val container =
        PostgreSQLContainer<Nothing>(DockerImageName.parse(DOCKER_IMAGE_NAME)).apply {
            withReuse(false)
            withDatabaseName(postgresProperties.databaseName)
            withUsername(postgresProperties.adminUser)
            withPassword("postgres")
            waitingFor(Wait.defaultWaitStrategy())
            start()
        }

    val dataSource: HikariDataSource by lazy {
        HikariDataSource(
            DatabaseTestConfig.hikariPostgresConfig(container.host, container.getMappedPort(5432)).apply {
                connectionInitSql = """SET ROLE "${PropertiesConfig.PostgresProperties().adminUser}""""
            },
        )
    }

    val scheduledTaskRepository: ScheduledTaskRepository by lazy {
        spyk(ScheduledTaskRepository(dataSource))
    }

    override suspend fun beforeSpec(spec: Spec) {
        container.start()
        dataSource.connection.use { connection ->
            connection.createStatement().use {
                val role = postgresProperties.user.replace("\"", "\"\"")
                it.execute("CREATE ROLE \"$role\"")
            }
        }
        Flyway
            .configure()
            .dataSource(dataSource)
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
