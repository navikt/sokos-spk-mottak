package no.nav.sokos.spk.mottak

import io.kotest.core.spec.style.ShouldSpec
import io.mockk.mockk
import no.nav.sokos.spk.mottak.listener.PostgresListener
import no.nav.sokos.spk.mottak.service.AvstemmingService
import no.nav.sokos.spk.mottak.service.ReadAndParseFileService
import no.nav.sokos.spk.mottak.service.SendTrekkTransaksjonToOppdragZService
import no.nav.sokos.spk.mottak.service.SendUtbetalingTransaksjonToOppdragZService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import no.nav.sokos.spk.mottak.service.WriteToFileService

internal class SchedulerTest :
    ShouldSpec({
        extensions(PostgresListener)

        val readAndParseFileService = mockk<ReadAndParseFileService>()
        val validateTransaksjonService = mockk<ValidateTransaksjonService>()
        val writeToFileService = mockk<WriteToFileService>()
        val sendUtbetalingTransaksjonToOppdragZService = mockk<SendUtbetalingTransaksjonToOppdragZService>()
        val sendTrekkTransaksjonToOppdragZService = mockk<SendTrekkTransaksjonToOppdragZService>()
        val avstemmingService = mockk<AvstemmingService>()

//        should("skal starte skedulering og trigge jobber") {
//            every { readAndParseFileService.readAndParseFile() } returns Unit
//            every { validateTransaksjonService.validateInnTransaksjon() } returns Unit
//            every { writeToFileService.writeReturnFile() } returns Unit
//            every { sendUtbetalingTransaksjonToOppdragZService.getUtbetalingTransaksjonAndSendToOppdragZ() } returns Unit
//            every { sendTrekkTransaksjonToOppdragZService.getTrekkTransaksjonAndSendToOppdrag() } returns Unit
//            every { avstemmingService.sendGrensesnittAvstemming() } returns Unit
//
//            val schedulerProperties =
//                PropertiesConfig
//                    .SchedulerProperties()
//                    .copy(
//                        readParseFileAndValidateTransactionsCronPattern = "* * * * * *",
//                        sendUtbetalingTransaksjonToOppdragZCronPattern = "* * * * * *",
//                        sendTrekkTransaksjonToOppdragZCronPattern = "* * * * * *",
//                        grensesnittAvstemmingCronPattern = "* * * * * *",
//                    )
//            val readParseFileAndValidateTransactionsTask =
//                JobTaskConfig.recurringReadParseFileAndValidateTransactionsTask(
//                    readAndParseFileService,
//                    validateTransaksjonService,
//                    writeToFileService,
//                    schedulerProperties,
//                )
//            val sendUtbetalingTransaksjonTilOppdragTask = JobTaskConfig.recurringSendUtbetalingTransaksjonToOppdragZTask(sendUtbetalingTransaksjonToOppdragZService, schedulerProperties)
//            val sendTrekkTransaksjonTilOppdragTask = JobTaskConfig.recurringSendTrekkTransaksjonToOppdragZTask(sendTrekkTransaksjonToOppdragZService, schedulerProperties)
//            val avstemmingTask = JobTaskConfig.recurringGrensesnittAvstemmingTask(avstemmingService, schedulerProperties)
//
//            val scheduler =
//                Scheduler
//                    .create(PostgresListener.dataSource)
//                    .startTasks(
//                        readParseFileAndValidateTransactionsTask,
//                        sendUtbetalingTransaksjonTilOppdragTask,
//                        sendTrekkTransaksjonTilOppdragTask,
//                        avstemmingTask,
//                    ).failureLogging(LogLevel.ERROR, true)
//                    .build()
//
//            runBlocking {
//                scheduler.start()
//                delay(12000)
//                scheduler.stop()
//            }
//
//            verify { readAndParseFileService.readAndParseFile() }
//            verify { validateTransaksjonService.validateInnTransaksjon() }
//            verify { sendUtbetalingTransaksjonToOppdragZService.getUtbetalingTransaksjonAndSendToOppdragZ() }
//            verify { sendTrekkTransaksjonToOppdragZService.getTrekkTransaksjonAndSendToOppdrag() }
//            verify { avstemmingService.sendGrensesnittAvstemming() }
//        }
    })
