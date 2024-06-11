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
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import no.nav.sokos.spk.mottak.service.WriteToFileService

internal class SchedulerTest : ShouldSpec({
    extensions(PostgresListener)

    val readAndParseFileService = mockk<ReadAndParseFileService>()
    val validateTransaksjonService = mockk<ValidateTransaksjonService>()
    val writeToFileService = mockk<WriteToFileService>()

    should("skal starte skedulering og trigge jobber") {
        every { readAndParseFileService.readAndParseFile() } returns Unit
        every { validateTransaksjonService.validateInnTransaksjon() } returns Unit
        every { writeToFileService.writeReturnFile() } returns Unit

        val schedulerProperties =
            PropertiesConfig.SchedulerProperties()
                .copy(
                    readAndParseFileCronPattern = "* * * * * *",
                    validateTransaksjonCronPattern = "* * * * * *",
                )
        val readAndParseFileTask = JobTaskConfig.recurringReadAndParseFileTask(readAndParseFileService, schedulerProperties)
        val validateTransaksjonTask = JobTaskConfig.recurringValidateTransaksjonTask(validateTransaksjonService, writeToFileService, schedulerProperties)

        val scheduler =
            Scheduler.create(PostgresListener.dataSource)
                .startTasks(readAndParseFileTask, validateTransaksjonTask)
                .failureLogging(LogLevel.ERROR, true)
                .build()

        runBlocking {
            scheduler.start()
            delay(12000)
            scheduler.stop()
        }

        verify { readAndParseFileService.readAndParseFile() }
        verify { validateTransaksjonService.validateInnTransaksjon() }
    }
})
