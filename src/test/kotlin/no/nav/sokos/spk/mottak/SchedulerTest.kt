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
import no.nav.sokos.spk.mottak.service.ReadFileService
import no.nav.sokos.spk.mottak.service.ScheduledTaskService
import no.nav.sokos.spk.mottak.service.SendAvregningsreturService
import no.nav.sokos.spk.mottak.service.SendInnlesningsreturService
import no.nav.sokos.spk.mottak.service.SendTrekkService
import no.nav.sokos.spk.mottak.service.SendUtbetalingService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService

internal class SchedulerTest :
    ShouldSpec({
        extensions(PostgresListener)

        val readFileService = mockk<ReadFileService>()
        val validateTransaksjonService = mockk<ValidateTransaksjonService>()
        val sendInnlesningsreturService = mockk<SendInnlesningsreturService>()
        val sendUtbetalingService = mockk<SendUtbetalingService>()
        val sendTrekkService = mockk<SendTrekkService>()
        val avstemmingService = mockk<AvstemmingService>()
        val sendAvregningsreturService = mockk<SendAvregningsreturService>()
        val scheduledTaskService = mockk<ScheduledTaskService>()

        should("skal starte skedulering og trigge jobber") {
            every { readFileService.readAndParseFile() } returns Unit
            every { validateTransaksjonService.validateInnTransaksjon() } returns Unit
            every { sendInnlesningsreturService.writeInnlesningsreturFile() } returns Unit
            every { sendUtbetalingService.getUtbetalingTransaksjonAndSendToOppdragZ() } returns Unit
            every { sendTrekkService.getTrekkTransaksjonAndSendToOppdrag() } returns Unit
            every { avstemmingService.sendGrensesnittAvstemming(any()) } returns Unit
            every { scheduledTaskService.insertScheduledTaskHistory(any(), any()) } returns Unit
            every { sendAvregningsreturService.writeAvregningsreturFile() } returns Unit

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
                    readFileService,
                    validateTransaksjonService,
                    sendInnlesningsreturService,
                    scheduledTaskService,
                    schedulerProperties,
                )
            val sendUtbetalingTransaksjonTilOppdragTask =
                JobTaskConfig.recurringSendUtbetalingTransaksjonToOppdragZTask(sendUtbetalingService, scheduledTaskService, schedulerProperties)
            val sendTrekkTransaksjonTilOppdragTask = JobTaskConfig.recurringSendTrekkTransaksjonToOppdragZTask(sendTrekkService, scheduledTaskService, schedulerProperties)
            val avstemmingTask = JobTaskConfig.recurringGrensesnittAvstemmingTask(avstemmingService, scheduledTaskService, schedulerProperties)
            val writeAvregningsreturFileTask = JobTaskConfig.recurringWriteAvregningsreturFileTask(sendAvregningsreturService, scheduledTaskService, schedulerProperties)

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

            verify { readFileService.readAndParseFile() }
            verify { validateTransaksjonService.validateInnTransaksjon() }
            verify { sendUtbetalingService.getUtbetalingTransaksjonAndSendToOppdragZ() }
            verify { sendTrekkService.getTrekkTransaksjonAndSendToOppdrag() }
            verify { avstemmingService.sendGrensesnittAvstemming(any()) }
            verify { sendAvregningsreturService.writeAvregningsreturFile() }
            verify { scheduledTaskService.insertScheduledTaskHistory(any(), any()) }
        }
    })
