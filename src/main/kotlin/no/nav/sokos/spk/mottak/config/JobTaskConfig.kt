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
import no.nav.sokos.spk.mottak.service.SendUtbetalingTransaksjonService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import no.nav.sokos.spk.mottak.service.WriteToFileService
import java.time.Duration
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

object JobTaskConfig {
    fun scheduler(dataSource: HikariDataSource = DatabaseConfig.postgresDataSource()): Scheduler =
        Scheduler
            .create(dataSource)
            .startTasks(recurringReadAndParseFileTask(), recurringValidateTransaksjonTask())
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

    internal fun recurringSendTransaksjonTilOppdragTask(
        sendUtbetalingTransaksjonService: SendUtbetalingTransaksjonService = SendUtbetalingTransaksjonService(),
        schedulerProperties: PropertiesConfig.SchedulerProperties = PropertiesConfig.SchedulerProperties(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("sendUtbetalingTransaksjonTilOppdrag", cron(schedulerProperties.validateTransaksjonCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                showLogLocalTime = showLog(showLogLocalTime, instance, context)
                // sendUtbetalingTransaksjonService.hentUtbetalingTransaksjonOgSendTilOppdrag()
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
