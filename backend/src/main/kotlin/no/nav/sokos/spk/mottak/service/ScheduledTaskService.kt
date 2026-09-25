package no.nav.sokos.spk.mottak.service

import kotlin.time.ExperimentalTime
import kotlin.time.Instant

import com.zaxxer.hikari.HikariDataSource

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.dto.JobTaskInfo
import no.nav.sokos.spk.mottak.repository.ScheduledTaskRepository

@OptIn(ExperimentalTime::class)
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
                Instant.fromEpochMilliseconds(it.executionTime.toInstant().toEpochMilli()),
                it.picked,
                it.pickedBy,
                it.lastFailure?.let { failure -> Instant.fromEpochMilliseconds(failure.toInstant().toEpochMilli()) },
                it.lastSuccess?.let { lastSuccess -> Instant.fromEpochMilliseconds(lastSuccess.toInstant().toEpochMilli()) },
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
