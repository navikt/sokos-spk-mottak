package no.nav.sokos.spk.mottak.config

import com.github.kagkarlsson.scheduler.ScheduledExecutionsFilter
import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.github.kagkarlsson.scheduler.logging.LogLevel
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules.cron
import com.zaxxer.hikari.HikariDataSource
import kotlinx.datetime.toKotlinInstant
import kotlinx.serialization.json.Json
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.api.model.AvstemmingRequest
import no.nav.sokos.spk.mottak.api.model.JobTask
import no.nav.sokos.spk.mottak.service.AvstemmingService
import no.nav.sokos.spk.mottak.service.ReadAndParseFileService
import no.nav.sokos.spk.mottak.service.SendTrekkTransaksjonToOppdragZService
import no.nav.sokos.spk.mottak.service.SendUtbetalingTransaksjonToOppdragZService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import no.nav.sokos.spk.mottak.service.WriteToFileService
import no.nav.sokos.spk.mottak.util.CallIdUtils.withCallId
import java.time.Duration
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

object JobTaskConfig {
    fun scheduler(dataSource: HikariDataSource = DatabaseConfig.postgresDataSource()): Scheduler =
        Scheduler
            .create(dataSource)
            .enableImmediateExecution()
            .registerShutdownHook()
            .startTasks(
                recurringReadParseFileAndValidateTransactionsTask(),
                recurringSendUtbetalingTransaksjonToOppdragZTask(),
                recurringSendTrekkTransaksjonToOppdragZTask(),
                recurringGrensesnittAvstemmingTask(),
            ).failureLogging(LogLevel.ERROR, true)
            .build()

    internal fun recurringReadParseFileAndValidateTransactionsTask(
        readAndParseFileService: ReadAndParseFileService = ReadAndParseFileService(),
        validateTransaksjonService: ValidateTransaksjonService = ValidateTransaksjonService(),
        writeToFileService: WriteToFileService = WriteToFileService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("readParseFileAndValidateTransactions", cron(schedulerProperties.readParseFileAndValidateTransactionsCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                withCallId {
                    showLogLocalTime = showLog(showLogLocalTime, instance, context)
                    readAndParseFileService.readAndParseFile()
                    validateTransaksjonService.validateInnTransaksjon()
                    writeToFileService.writeReturnFile()
                }
            }
    }

    internal fun recurringSendUtbetalingTransaksjonToOppdragZTask(
        sendUtbetalingTransaksjonToOppdragZService: SendUtbetalingTransaksjonToOppdragZService =
            SendUtbetalingTransaksjonToOppdragZService(
                mqBatchSize = PropertiesConfig.MQProperties().mqBatchSize,
            ),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("sendUtbetalingTransaksjonToOppdragZ", cron(schedulerProperties.sendUtbetalingTransaksjonToOppdragZCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                withCallId {
                    showLogLocalTime = showLog(showLogLocalTime, instance, context)
                    sendUtbetalingTransaksjonToOppdragZService.getUtbetalingTransaksjonAndSendToOppdragZ()
                }
            }
    }

    internal fun recurringSendTrekkTransaksjonToOppdragZTask(
        sendTrekkTransaksjonToOppdragZService: SendTrekkTransaksjonToOppdragZService =
            SendTrekkTransaksjonToOppdragZService(
                mqBatchSize = PropertiesConfig.MQProperties().mqBatchSize,
            ),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("sendTrekkTransaksjonToOppdragZ", cron(schedulerProperties.sendTrekkTransaksjonToOppdragZCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                withCallId {
                    showLogLocalTime = showLog(showLogLocalTime, instance, context)
                    sendTrekkTransaksjonToOppdragZService.getTrekkTransaksjonAndSendToOppdrag()
                }
            }
    }

    internal fun recurringGrensesnittAvstemmingTask(
        avstemmingService: AvstemmingService = AvstemmingService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<String> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("grensesnittAvstemming", cron(schedulerProperties.grensesnittAvstemmingCronPattern), String::class.java)
            .execute { instance: TaskInstance<String>, context: ExecutionContext ->
                withCallId {
                    showLogLocalTime = showLog(showLogLocalTime, instance, context)
                    val request = instance.data?.let { Json.decodeFromString<AvstemmingRequest>(instance.data) }
                    avstemmingService.sendGrensesnittAvstemming(request)
                }
            }
    }

    internal fun schedulerWithTypeInformation(): List<JobTask> {
        DatabaseConfig.postgresDataSource().use { dataSource ->
            val schedulerClient = SchedulerClient.Builder.create(dataSource).build()
            return schedulerClient
                .getScheduledExecutions(ScheduledExecutionsFilter.all())
                .map {
                    JobTask(
                        it.taskInstance.id,
                        it.taskInstance.taskName,
                        it.executionTime.toKotlinInstant(),
                        it.isPicked,
                        it.pickedBy,
                        it.lastFailure?.toKotlinInstant(),
                        it.lastSuccess?.toKotlinInstant(),
                    )
                }
        }
    }

    private fun <T> showLog(
        localtime: LocalDateTime,
        instance: TaskInstance<T>,
        context: ExecutionContext,
    ): LocalDateTime {
        if (localtime.plusMinutes(Duration.ofMinutes(5).toMinutes()).isBefore(LocalDateTime.now())) {
            logger.info { "Kjør skedulering med instans: $instance, jobbnavn: $context" }
            return LocalDateTime.now()
        }
        return localtime
    }
}
