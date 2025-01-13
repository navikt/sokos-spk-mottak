package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.listener.PostgresListener
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

internal class ScheduledTaskServiceTest : FunSpec({
    extensions(PostgresListener)

    val scheduledTaskService: ScheduledTaskService by lazy { ScheduledTaskService(dataSource = PostgresListener.dataSource) }

    test("getScheduledTaskInformation should return a list of JobTaskInfo") {
        PostgresListener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/postgres/scheduled_tasks.sql")))
            session.update(queryOf(readFromResource("/database/postgres/scheduled_tasks_history.sql")))
        }

        val result = scheduledTaskService.getScheduledTaskInformation()
        result.size shouldBe 4
        result.forEach { task ->
            listOf(
                "readParseFileAndValidateTransactions",
                "sendUtbetalingTransaksjonToOppdragZ",
                "sendTrekkTransaksjonToOppdragZ",
                "grensesnittAvstemming",
            ).contains(task.taskName) shouldBe true
            task.taskId shouldBe "recurring"
            task.isPicked shouldBe false
            task.pickedBy shouldBe null

            if (task.taskName == "readParseFileAndValidateTransactions") {
                task.ident shouldBe "W12345"
            }
        }
    }

    test("insertScheduledTaskHistory should insert a scheduled task with Ident") {
        scheduledTaskService.insertScheduledTaskHistory("W12345", "grensesnittAvstemming")

        val scheduledTaskMap = PostgresListener.scheduledTaskRepository.getLastScheduledTask()
        scheduledTaskMap["grensesnittAvstemming"]?.taskName shouldBe "grensesnittAvstemming"
        scheduledTaskMap["grensesnittAvstemming"]?.ident shouldBe "W12345"
    }
})
