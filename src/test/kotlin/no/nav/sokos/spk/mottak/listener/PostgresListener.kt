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
    val container =
        PostgreSQLContainer<Nothing>(DockerImageName.parse(DOCKER_IMAGE_NAME)).apply {
            withReuse(false)
            withUsername("test-admin")
            waitingFor(Wait.defaultWaitStrategy())
            start()
        }

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
