package no.nav.sokos.spk.mottak.api

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.sokos.spk.mottak.api.model.AvstemmingRequest
import no.nav.sokos.spk.mottak.config.JobTaskConfig
import java.time.Instant

private const val RECURRING = "recurring"

fun Route.mottakApi(scheduler: Scheduler = JobTaskConfig.scheduler()) {
    route("api/v1") {
        post("readParseFileAndValidateTransactions") {
            call.launch(Dispatchers.IO) {
                val task = JobTaskConfig.recurringReadParseFileAndValidateTransactionsTask()
                scheduler.reschedule(task.instance(RECURRING), Instant.now())
            }
            call.respond(HttpStatusCode.Accepted, "ReadAndParseFile av filer har startet, sjekk logger for status")
        }

        post("sendUtbetalingTransaksjonToOppdragZ") {
            call.launch(Dispatchers.IO) {
                val task = JobTaskConfig.recurringSendUtbetalingTransaksjonToOppdragZTask()
                scheduler.reschedule(task.instance(RECURRING), Instant.now())
            }
            call.respond(HttpStatusCode.Accepted, "SendUtbetalingTransaksjonTilOppdrag har startet, sjekk logger for status")
        }

        post("sendTrekkTransaksjonToOppdragZ") {
            call.launch(Dispatchers.IO) {
                val task = JobTaskConfig.recurringSendTrekkTransaksjonToOppdragZTask()
                scheduler.reschedule(task.instance(RECURRING), Instant.now())
            }
            call.respond(HttpStatusCode.Accepted, "SendTrekkTransaksjonTilOppdrag har startet, sjekk logger for status")
        }

        post("avstemming") {
            val request = call.receive<AvstemmingRequest>()
            call.launch(Dispatchers.IO) {
                val task = JobTaskConfig.recurringGrensesnittAvstemmingTask()
                val requestData = Json.encodeToString(request)
                scheduler.reschedule(task.instance(RECURRING), Instant.now(), requestData)
            }
            call.respond(HttpStatusCode.Accepted, "GrensesnittAvstemming har startet, sjekk logger for status")
        }

        get("jobTaskInfo") {
            call.respond(HttpStatusCode.OK, JobTaskConfig.schedulerWithTypeInformation())
        }
    }
}
