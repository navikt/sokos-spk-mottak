package no.nav.sokos.spk.mottak.service

import com.github.kagkarlsson.scheduler.ScheduledExecutionsFilter
import com.github.kagkarlsson.scheduler.SchedulerClient
import com.zaxxer.hikari.HikariDataSource
import kotlinx.datetime.toKotlinInstant
import no.nav.sokos.spk.mottak.dto.JobTaskInfo
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.repository.ScheduledTaskRepository

class ScheduledTaskService(
    private val dataSource: HikariDataSource = DatabaseConfig.postgresDataSource(),
    private val scheduledTaskRepository: ScheduledTaskRepository = ScheduledTaskRepository(dataSource),
) {
    fun getScheduledTaskInformation(): List<JobTaskInfo> {
        val scheduledTaskMap = scheduledTaskRepository.getLastScheduledTask()
        return dataSource.use { dataSource ->
            val schedulerClient = SchedulerClient.Builder.create(dataSource).build()
            schedulerClient
                .getScheduledExecutions(ScheduledExecutionsFilter.all())
                .map {
                    JobTaskInfo(
                        it.taskInstance.id,
                        it.taskInstance.taskName,
                        it.executionTime.toKotlinInstant(),
                        it.isPicked,
                        it.pickedBy,
                        it.lastFailure?.toKotlinInstant(),
                        it.lastSuccess?.toKotlinInstant(),
                        scheduledTaskMap[it.taskInstance.taskName]?.ident,
                    )
                }
        }
    }

    fun insertScheduledTaskHistory(
        ident: String,
        taskName: String,
    ) {
        scheduledTaskRepository.insert(ident, taskName)
    }
}
