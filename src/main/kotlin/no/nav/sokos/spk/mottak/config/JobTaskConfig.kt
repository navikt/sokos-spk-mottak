package no.nav.sokos.spk.mottak.config

import java.time.Duration
import java.time.LocalDateTime

import kotlinx.serialization.json.Json

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.logging.LogLevel
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules.cron
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.api.model.AvstemmingRequest
import no.nav.sokos.spk.mottak.service.AvstemmingService
import no.nav.sokos.spk.mottak.service.ReadFileService
import no.nav.sokos.spk.mottak.service.ScheduledTaskService
import no.nav.sokos.spk.mottak.service.SendAvregningsreturService
import no.nav.sokos.spk.mottak.service.SendInnlesningsreturService
import no.nav.sokos.spk.mottak.service.SendTrekkService
import no.nav.sokos.spk.mottak.service.SendUtbetalingService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import no.nav.sokos.spk.mottak.util.CallIdUtils.withCallId

private val logger = KotlinLogging.logger {}

private const val JOB_TASK_GRENSESNITT_AVSTEMMING = "grensesnittAvstemming"
private const val JOB_TASK_SEND_TREKK_TRANSAKSJON_TO_OPPDRAGZ = "sendTrekkTransaksjonToOppdragZ"
private const val JOB_TASK_SEND_UTBETALING_TRANSAKSJON_TO_OPPDRAGZ = "sendUtbetalingTransaksjonToOppdragZ"
private const val JOB_TASK_READ_PARSE_FILE_AND_VALIDATE_TRANSACTIONS = "readParseFileAndValidateTransactions"
private const val JOB_TASK_WRITE_AVREGNINGSRETUR_FILE = "writeAvregningsreturFile"

object JobTaskConfig {
    fun scheduler(dataSource: HikariDataSource = DatabaseConfig.postgresDataSource): Scheduler =
        Scheduler
            .create(dataSource)
            .enableImmediateExecution()
            .registerShutdownHook()
            .startTasks(
                recurringReadParseFileAndValidateTransactionsTask(),
                recurringSendUtbetalingTransaksjonToOppdragZTask(),
                recurringSendTrekkTransaksjonToOppdragZTask(),
                recurringGrensesnittAvstemmingTask(),
                recurringWriteAvregningsreturFileTask(),
            ).failureLogging(LogLevel.ERROR, true)
            .build()

    internal fun recurringReadParseFileAndValidateTransactionsTask(
        readFileService: ReadFileService = ReadFileService(),
        validateTransaksjonService: ValidateTransaksjonService = ValidateTransaksjonService(),
        sendInnlesningsreturService: SendInnlesningsreturService = SendInnlesningsreturService(),
        scheduledTaskService: ScheduledTaskService = ScheduledTaskService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<String> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring(JOB_TASK_READ_PARSE_FILE_AND_VALIDATE_TRANSACTIONS, cron(schedulerProperties.readParseFileAndValidateTransactionsCronPattern), String::class.java)
            .execute { instance: TaskInstance<String>, context: ExecutionContext ->
                withCallId {
                    showLogLocalTime = showLog(showLogLocalTime, instance, context)
                    val ident = instance.data ?: PropertiesConfig.Configuration().naisAppName
                    scheduledTaskService.insertScheduledTaskHistory(ident, JOB_TASK_READ_PARSE_FILE_AND_VALIDATE_TRANSACTIONS)
                    readFileService.readAndParseFile()
                    validateTransaksjonService.validateInnTransaksjon()
                    sendInnlesningsreturService.writeInnlesningsreturFile()
                }
            }
    }

    internal fun recurringSendUtbetalingTransaksjonToOppdragZTask(
        sendUtbetalingService: SendUtbetalingService =
            SendUtbetalingService(
                mqBatchSize = PropertiesConfig.MQProperties().mqBatchSize,
            ),
        scheduledTaskService: ScheduledTaskService = ScheduledTaskService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<String> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring(JOB_TASK_SEND_UTBETALING_TRANSAKSJON_TO_OPPDRAGZ, cron(schedulerProperties.sendUtbetalingTransaksjonToOppdragZCronPattern), String::class.java)
            .execute { instance: TaskInstance<String>, context: ExecutionContext ->
                withCallId {
                    showLogLocalTime = showLog(showLogLocalTime, instance, context)
                    val ident = instance.data ?: PropertiesConfig.Configuration().naisAppName
                    scheduledTaskService.insertScheduledTaskHistory(ident, JOB_TASK_SEND_UTBETALING_TRANSAKSJON_TO_OPPDRAGZ)
                    sendUtbetalingService.getUtbetalingTransaksjonAndSendToOppdragZ()
                }
            }
    }

    internal fun recurringSendTrekkTransaksjonToOppdragZTask(
        sendTrekkService: SendTrekkService =
            SendTrekkService(
                mqBatchSize = PropertiesConfig.MQProperties().mqBatchSize,
            ),
        scheduledTaskService: ScheduledTaskService = ScheduledTaskService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<String> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring(JOB_TASK_SEND_TREKK_TRANSAKSJON_TO_OPPDRAGZ, cron(schedulerProperties.sendTrekkTransaksjonToOppdragZCronPattern), String::class.java)
            .execute { instance: TaskInstance<String>, context: ExecutionContext ->
                withCallId {
                    showLogLocalTime = showLog(showLogLocalTime, instance, context)
                    val ident = instance.data ?: PropertiesConfig.Configuration().naisAppName
                    scheduledTaskService.insertScheduledTaskHistory(ident, JOB_TASK_SEND_TREKK_TRANSAKSJON_TO_OPPDRAGZ)
                    sendTrekkService.getTrekkTransaksjonAndSendToOppdrag()
                }
            }
    }

    internal fun recurringGrensesnittAvstemmingTask(
        avstemmingService: AvstemmingService = AvstemmingService(),
        scheduledTaskService: ScheduledTaskService = ScheduledTaskService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<String> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring(JOB_TASK_GRENSESNITT_AVSTEMMING, cron(schedulerProperties.grensesnittAvstemmingCronPattern), String::class.java)
            .execute { instance: TaskInstance<String>, context: ExecutionContext ->
                withCallId {
                    showLogLocalTime = showLog(showLogLocalTime, instance, context)
                    val taskData = instance.data?.let { Json.decodeFromString<Pair<String, AvstemmingRequest>>(it) }
                    scheduledTaskService.insertScheduledTaskHistory(taskData?.first ?: PropertiesConfig.Configuration().naisAppName, JOB_TASK_GRENSESNITT_AVSTEMMING)
                    avstemmingService.sendGrensesnittAvstemming(taskData?.second)
                }
            }
    }

    internal fun recurringWriteAvregningsreturFileTask(
        sendAvregningsreturService: SendAvregningsreturService = SendAvregningsreturService(),
        scheduledTaskService: ScheduledTaskService = ScheduledTaskService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<String> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring(JOB_TASK_WRITE_AVREGNINGSRETUR_FILE, cron(schedulerProperties.writeAvregningsreturFileCronPattern), String::class.java)
            .execute { instance: TaskInstance<String>, context: ExecutionContext ->
                withCallId {
                    showLogLocalTime = showLog(showLogLocalTime, instance, context)
                    val ident = instance.data ?: PropertiesConfig.Configuration().naisAppName
                    scheduledTaskService.insertScheduledTaskHistory(ident, JOB_TASK_WRITE_AVREGNINGSRETUR_FILE)
                    sendAvregningsreturService.writeAvregningsreturFile()
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
