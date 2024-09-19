package no.nav.sokos.spk.mottak.config

import com.github.kagkarlsson.scheduler.Scheduler
import com.github.kagkarlsson.scheduler.logging.LogLevel
import com.github.kagkarlsson.scheduler.task.ExecutionContext
import com.github.kagkarlsson.scheduler.task.TaskInstance
import com.github.kagkarlsson.scheduler.task.helper.RecurringTask
import com.github.kagkarlsson.scheduler.task.helper.Tasks
import com.github.kagkarlsson.scheduler.task.schedule.Schedules.cron
import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.service.ReadAndParseFileService
import no.nav.sokos.spk.mottak.service.SendTrekkService
import no.nav.sokos.spk.mottak.service.SendTrekkTransaksjonToOppdragService
import no.nav.sokos.spk.mottak.service.SendUtbetalingService
import no.nav.sokos.spk.mottak.service.SendUtbetalingTransaksjonToOppdragService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import no.nav.sokos.spk.mottak.service.WriteToFileService
import java.time.Duration
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

object JobTaskConfig {
    fun scheduler(dataSource: HikariDataSource = DatabaseConfig.postgresDataSource()): Scheduler =
        Scheduler
            .create(dataSource)
            .startTasks(recurringValidateTransaksjonTask())
            .failureLogging(LogLevel.ERROR, true)
            .build()

    internal fun recurringReadAndParseFileTask(
        readAndParseFileService: ReadAndParseFileService = ReadAndParseFileService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("readAndParseFile", cron(schedulerProperties.readAndParseFileCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                showLogLocalTime = showLog(showLogLocalTime, instance, context)
                readAndParseFileService.readAndParseFile()
            }
    }

    internal fun recurringValidateTransaksjonTask(
        validateTransaksjonService: ValidateTransaksjonService = ValidateTransaksjonService(),
        writeToFileService: WriteToFileService = WriteToFileService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("validateTransaksjon", cron(schedulerProperties.validateTransaksjonCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                showLogLocalTime = showLog(showLogLocalTime, instance, context)
                validateTransaksjonService.validateInnTransaksjon()
                writeToFileService.writeReturnFile()
            }
    }

    internal fun recurringSendUtbetalingTransaksjonToOppdragTask(
        sendUtbetalingTransaksjonToOppdragService: SendUtbetalingTransaksjonToOppdragService = SendUtbetalingTransaksjonToOppdragService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("sendUtbetalingTransaksjonToOppdrag", cron(schedulerProperties.sendUtbetalingTransaksjonTilOppdragCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                showLogLocalTime = showLog(showLogLocalTime, instance, context)
                sendUtbetalingTransaksjonToOppdragService.fetchUtbetalingTransaksjonAndSendToOppdrag()
            }
    }

    internal fun recurringSendTrekkTransaksjonToOppdragTask(
        sendTrekkTransaksjonToOppdragService: SendTrekkTransaksjonToOppdragService = SendTrekkTransaksjonToOppdragService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("sendTrekkTransaksjonToOppdrag", cron(schedulerProperties.sendTrekkTransaksjonTilOppdragCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                showLogLocalTime = showLog(showLogLocalTime, instance, context)
                sendTrekkTransaksjonToOppdragService.fetchTrekkTransaksjonAndSendToOppdrag()
            }
    }

    internal fun outboxTrekkTask(
        sendTrekkService: SendTrekkService = SendTrekkService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("sendTrekk", cron(schedulerProperties.sendTransaksjonCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                showLogLocalTime = showLog(showLogLocalTime, instance, context)
                sendTrekkService.sendToOppdrag()
            }
    }

    internal fun outboxUtbetalingTask(
        sendUtbetalingService: SendUtbetalingService = SendUtbetalingService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("sendUtbetaling", cron(schedulerProperties.sendTransaksjonCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                showLogLocalTime = showLog(showLogLocalTime, instance, context)
                sendUtbetalingService.sendToOppdrag()
            }
    }

    private fun showLog(
        localtime: LocalDateTime,
        instance: TaskInstance<Void>,
        context: ExecutionContext,
    ): LocalDateTime {
        if (localtime.plusMinutes(Duration.ofMinutes(5).toMinutes()).isBefore(LocalDateTime.now())) {
            logger.info { "Kjør skedulering med instans: $instance, jobbnavn: $context" }
            return LocalDateTime.now()
        }
        return localtime
    }
}
