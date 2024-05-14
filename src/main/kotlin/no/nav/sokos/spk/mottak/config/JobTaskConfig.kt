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
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import java.time.Duration
import java.time.LocalDateTime

private val logger = KotlinLogging.logger {}

object JobTaskConfig {
    fun scheduler(dataSource: HikariDataSource = DatabaseConfig.postgresDataSource()): Scheduler {
        return Scheduler.create(dataSource)
            .startTasks(recurringReadAndParseFileTask(), recurringValidateTransaksjonTask())
            .failureLogging(LogLevel.ERROR, true)
            .build()
    }

    internal fun recurringReadAndParseFileTask(
        readAndParseFileService: ReadAndParseFileService = ReadAndParseFileService(),
        schedulerConfig: PropertiesConfig.SchedulerConfig = PropertiesConfig.SchedulerConfig(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("readAndParseFile", cron(schedulerConfig.readAndParseFileCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                showLogLocalTime = showLog(showLogLocalTime, instance, context)
                readAndParseFileService.readAndParseFile()
            }
    }

    internal fun recurringValidateTransaksjonTask(
        validateTransaksjonService: ValidateTransaksjonService = ValidateTransaksjonService(),
        schedulerConfig: PropertiesConfig.SchedulerConfig = PropertiesConfig.SchedulerConfig(),
    ): RecurringTask<Void> {
        var showLogLocalTime = LocalDateTime.now()
        return Tasks
            .recurring("validateTransaksjon", cron(schedulerConfig.validateTransaksjonCronPattern))
            .execute { instance: TaskInstance<Void>, context: ExecutionContext ->
                showLogLocalTime = showLog(showLogLocalTime, instance, context)
                validateTransaksjonService.validateInnTransaksjon()
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