package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import kotlinx.datetime.toKotlinInstant
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.dto.JobTaskInfo
import no.nav.sokos.spk.mottak.repository.ScheduledTaskRepository

class ScheduledTaskService(
    private val dataSource: HikariDataSource = DatabaseConfig.postgresDataSource,
    private val scheduledTaskRepository: ScheduledTaskRepository = ScheduledTaskRepository(dataSource),
) {
    fun getScheduledTaskInformation(): List<JobTaskInfo> {
        val scheduledTaskMap = scheduledTaskRepository.getLastScheduledTask()
        return scheduledTaskRepository.getAllScheduledTasks().map {
            JobTaskInfo(
                it.taskInstance,
                it.taskName,
                it.executionTime.toInstant().toKotlinInstant(),
                it.picked,
                it.pickedBy,
                it.lastFailure?.toInstant()?.toKotlinInstant(),
                it.lastSuccess?.toInstant()?.toKotlinInstant(),
                scheduledTaskMap[it.taskName]?.ident,
            )
        }
    }

    fun insertScheduledTaskHistory(
        ident: String,
        taskName: String,
    ) {
        scheduledTaskRepository.insert(ident, taskName)
    }
}
