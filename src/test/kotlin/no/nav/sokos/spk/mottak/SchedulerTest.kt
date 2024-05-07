package no.nav.sokos.spk.mottak

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.logging.LogLevel
import io.kotest.common.runBlocking
import io.kotest.core.spec.style.ShouldSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import no.nav.sokos.spk.mottak.config.JobTaskConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.listener.PostgresListener
import no.nav.sokos.spk.mottak.service.ReadAndParseFileService
import java.time.Duration.ofSeconds
import java.time.Instant

class SchedulerTest : ShouldSpec({
    extensions(PostgresListener)

    val readAndParseFileService = mockk<ReadAndParseFileService>()

    should("skal starte å trigge readAndParseFileService") {
        every { readAndParseFileService.readAndParseFile() } returns Unit

        val schedulerConfig = PropertiesConfig.SchedulerConfig().copy(readAndParseFileCronPattern = "* * * * * *")
        val task = JobTaskConfig.recurringReadAndParseFileTask(readAndParseFileService, schedulerConfig)
        val scheduler =
            Scheduler.create(PostgresListener.dataSource)
                .startTasks(task)
                .failureLogging(LogLevel.ERROR, true)
                .build()

        val executionTime: Instant = Instant.now().plus(ofSeconds(1))

        runBlocking {
            scheduler.start()
            delay(12000)
            scheduler.stop()
        }
        verify { readAndParseFileService.readAndParseFile() }
    }
})
