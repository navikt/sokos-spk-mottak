package no.nav.sokos.spk.mottak

import kotlinx.coroutines.delay

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.logging.LogLevel.ERROR
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.engine.runBlocking
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import no.nav.sokos.spk.mottak.config.JobTaskConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.listener.PostgresListener
import no.nav.sokos.spk.mottak.service.AvstemmingService
import no.nav.sokos.spk.mottak.service.ReadAndParseFileService
import no.nav.sokos.spk.mottak.service.ScheduledTaskService
import no.nav.sokos.spk.mottak.service.SendTrekkTransaksjonToOppdragZService
import no.nav.sokos.spk.mottak.service.SendUtbetalingTransaksjonToOppdragZService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import no.nav.sokos.spk.mottak.service.WriteAvregningsreturFileService
import no.nav.sokos.spk.mottak.service.WriteInnlesningsreturFileService

internal class SchedulerTest :
    ShouldSpec({
        extensions(PostgresListener)

        val readAndParseFileService = mockk<ReadAndParseFileService>()
        val validateTransaksjonService = mockk<ValidateTransaksjonService>()
        val writeInnlesningsreturFileService = mockk<WriteInnlesningsreturFileService>()
        val sendUtbetalingTransaksjonToOppdragZService = mockk<SendUtbetalingTransaksjonToOppdragZService>()
        val sendTrekkTransaksjonToOppdragZService = mockk<SendTrekkTransaksjonToOppdragZService>()
        val avstemmingService = mockk<AvstemmingService>()
        val writeAvregningsreturFileService = mockk<WriteAvregningsreturFileService>()
        val scheduledTaskService = mockk<ScheduledTaskService>()

        should("skal starte skedulering og trigge jobber") {
            every { readAndParseFileService.readAndParseFile() } returns Unit
            every { validateTransaksjonService.validateInnTransaksjon() } returns Unit
            every { writeInnlesningsreturFileService.writeInnlesningsreturFile() } returns Unit
            every { sendUtbetalingTransaksjonToOppdragZService.getUtbetalingTransaksjonAndSendToOppdragZ() } returns Unit
            every { sendTrekkTransaksjonToOppdragZService.getTrekkTransaksjonAndSendToOppdrag() } returns Unit
            every { avstemmingService.sendGrensesnittAvstemming(any()) } returns Unit
            every { scheduledTaskService.insertScheduledTaskHistory(any(), any()) } returns Unit
            every { writeAvregningsreturFileService.writeAvregningsreturFile() } returns Unit

            val schedulerProperties =
                PropertiesConfig
                    .SchedulerProperties()
                    .copy(
                        readParseFileAndValidateTransactionsCronPattern = "* * * * * *",
                        sendUtbetalingTransaksjonToOppdragZCronPattern = "* * * * * *",
                        sendTrekkTransaksjonToOppdragZCronPattern = "* * * * * *",
                        grensesnittAvstemmingCronPattern = "* * * * * *",
                        writeAvregningsreturFileCronPattern = "* * * * * *",
                    )
            val readParseFileAndValidateTransactionsTask =
                JobTaskConfig.recurringReadParseFileAndValidateTransactionsTask(
                    readAndParseFileService,
                    validateTransaksjonService,
                    writeInnlesningsreturFileService,
                    scheduledTaskService,
                    schedulerProperties,
                )
            val sendUtbetalingTransaksjonTilOppdragTask =
                JobTaskConfig.recurringSendUtbetalingTransaksjonToOppdragZTask(sendUtbetalingTransaksjonToOppdragZService, scheduledTaskService, schedulerProperties)
            val sendTrekkTransaksjonTilOppdragTask = JobTaskConfig.recurringSendTrekkTransaksjonToOppdragZTask(sendTrekkTransaksjonToOppdragZService, scheduledTaskService, schedulerProperties)
            val avstemmingTask = JobTaskConfig.recurringGrensesnittAvstemmingTask(avstemmingService, scheduledTaskService, schedulerProperties)
            val writeAvregningsreturFileTask = JobTaskConfig.recurringWriteAvregningsreturFileTask(writeAvregningsreturFileService, scheduledTaskService, schedulerProperties)

            val scheduler =
                Scheduler
                    .create(PostgresListener.dataSource)
                    .startTasks(
                        readParseFileAndValidateTransactionsTask,
                        sendUtbetalingTransaksjonTilOppdragTask,
                        sendTrekkTransaksjonTilOppdragTask,
                        avstemmingTask,
                        writeAvregningsreturFileTask,
                    ).failureLogging(ERROR, true)
                    .build()

            runBlocking {
                scheduler.start()
                delay(12000)
                scheduler.stop()
            }

            verify { readAndParseFileService.readAndParseFile() }
            verify { validateTransaksjonService.validateInnTransaksjon() }
            verify { sendUtbetalingTransaksjonToOppdragZService.getUtbetalingTransaksjonAndSendToOppdragZ() }
            verify { sendTrekkTransaksjonToOppdragZService.getTrekkTransaksjonAndSendToOppdrag() }
            verify { avstemmingService.sendGrensesnittAvstemming(any()) }
            verify { writeAvregningsreturFileService.writeAvregningsreturFile() }
            verify { scheduledTaskService.insertScheduledTaskHistory(any(), any()) }
        }
    })
