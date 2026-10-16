package no.nav.sokos.spk.mottak.config

import kotlinx.serialization.json.Json

import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.Called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify

import no.nav.sokos.spk.mottak.api.model.AvstemmingRequest
import no.nav.sokos.spk.mottak.service.AvstemmingService
import no.nav.sokos.spk.mottak.service.ReadFileService
import no.nav.sokos.spk.mottak.service.ScheduledTaskService
import no.nav.sokos.spk.mottak.service.SendAvregningsreturService
import no.nav.sokos.spk.mottak.service.SendInnlesningsreturService
import no.nav.sokos.spk.mottak.service.SendTrekkService
import no.nav.sokos.spk.mottak.service.SendUtbetalingService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService

internal class JobTaskConfigTest :
    FunSpec({
        val taskNames =
            listOf(
                "readParseFileAndValidateTransactions",
                "sendUtbetalingTransaksjonToOppdragZ",
                "sendTrekkTransaksjonToOppdragZ",
                "grensesnittAvstemming",
                "writeAvregningsreturFile",
            )

        taskNames.forEach { taskName ->
            test("$taskName skips disabled executions and resumes when enabled") {
                val readFileService = mockk<ReadFileService>(relaxed = true)
                val validateService = mockk<ValidateTransaksjonService>(relaxed = true)
                val innlesningsreturService = mockk<SendInnlesningsreturService>(relaxed = true)
                val utbetalingService = mockk<SendUtbetalingService>(relaxed = true)
                val trekkService = mockk<SendTrekkService>(relaxed = true)
                val avstemmingService = mockk<AvstemmingService>(relaxed = true)
                val avregningsreturService = mockk<SendAvregningsreturService>(relaxed = true)
                val historyService = mockk<ScheduledTaskService>(relaxed = true)
                var enabled = false
                var toggleChecks = 0
                val schedulerEnabled = {
                    toggleChecks++
                    enabled
                }
                val task: RecurringTask<String> =
                    when (taskName) {
                        "readParseFileAndValidateTransactions" ->
                            JobTaskConfig.recurringReadParseFileAndValidateTransactionsTask(
                                readFileService,
                                validateService,
                                innlesningsreturService,
                                historyService,
                                schedulerEnabled = schedulerEnabled,
                            )
                        "sendUtbetalingTransaksjonToOppdragZ" ->
                            JobTaskConfig.recurringSendUtbetalingTransaksjonToOppdragZTask(
                                utbetalingService,
                                historyService,
                                schedulerEnabled = schedulerEnabled,
                            )
                        "sendTrekkTransaksjonToOppdragZ" ->
                            JobTaskConfig.recurringSendTrekkTransaksjonToOppdragZTask(
                                trekkService,
                                historyService,
                                schedulerEnabled = schedulerEnabled,
                            )
                        "grensesnittAvstemming" ->
                            JobTaskConfig.recurringGrensesnittAvstemmingTask(
                                avstemmingService,
                                historyService,
                                schedulerEnabled = schedulerEnabled,
                            )
                        else ->
                            JobTaskConfig.recurringWriteAvregningsreturFileTask(
                                avregningsreturService,
                                historyService,
                                schedulerEnabled = schedulerEnabled,
                            )
                    }
                val data = if (taskName == "grensesnittAvstemming") Json.encodeToString("test" to AvstemmingRequest(null, null)) else "test"
                val instance = task.instance("test", data)
                val context = mockk<ExecutionContext>()
                every { context.schedulerState.isShuttingDown } returns true

                task.execute(instance, context)
                toggleChecks shouldBe 0
                verify(exactly = 0) { historyService.insertScheduledTaskHistory(any(), any()) }
                verify {
                    listOf(
                        readFileService,
                        validateService,
                        innlesningsreturService,
                        utbetalingService,
                        trekkService,
                        avstemmingService,
                        avregningsreturService,
                    ) wasNot Called
                }

                every { context.schedulerState.isShuttingDown } returns false
                task.execute(instance, context)
                verify(exactly = 0) { historyService.insertScheduledTaskHistory(any(), any()) }
                verify {
                    listOf(
                        readFileService,
                        validateService,
                        innlesningsreturService,
                        utbetalingService,
                        trekkService,
                        avstemmingService,
                        avregningsreturService,
                    ) wasNot Called
                }

                enabled = true
                task.execute(instance, context)
                enabled = false
                task.execute(instance, context)

                toggleChecks shouldBe 3
                verify(exactly = 1) { historyService.insertScheduledTaskHistory("test", taskName) }
                when (taskName) {
                    "readParseFileAndValidateTransactions" -> {
                        verify(exactly = 1) { readFileService.readAndParseFile() }
                        verify(exactly = 1) { validateService.validateInnTransaksjon() }
                        verify(exactly = 1) { innlesningsreturService.writeInnlesningsreturFile() }
                    }
                    "sendUtbetalingTransaksjonToOppdragZ" -> verify(exactly = 1) { utbetalingService.getUtbetalingTransaksjonAndSendToOppdragZ() }
                    "sendTrekkTransaksjonToOppdragZ" -> verify(exactly = 1) { trekkService.getTrekkTransaksjonAndSendToOppdrag() }
                    "grensesnittAvstemming" -> verify(exactly = 1) { avstemmingService.sendGrensesnittAvstemming(any()) }
                    else -> verify(exactly = 1) { avregningsreturService.writeAvregningsreturFile() }
                }
            }
        }

        test("enabled job propagates failures to the scheduler") {
            val sendTrekkService = mockk<SendTrekkService>()
            val historyService = mockk<ScheduledTaskService>(relaxed = true)
            val failure = IllegalStateException("Sending failed")
            every { sendTrekkService.getTrekkTransaksjonAndSendToOppdrag() } throws failure
            val task =
                JobTaskConfig.recurringSendTrekkTransaksjonToOppdragZTask(
                    sendTrekkService,
                    historyService,
                    schedulerEnabled = { true },
                )
            val context = mockk<ExecutionContext>()
            every { context.schedulerState.isShuttingDown } returns false

            shouldThrow<IllegalStateException> {
                task.execute(task.instance("test", "test"), context)
            } shouldBe failure
        }
    })
