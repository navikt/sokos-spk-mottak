package no.nav.sokos.spk.mottak.api

import java.time.Instant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

import no.nav.sokos.spk.mottak.api.model.AvstemmingRequest
import no.nav.sokos.spk.mottak.config.JobTaskConfig
import no.nav.sokos.spk.mottak.security.AuthToken
import no.nav.sokos.spk.mottak.service.ScheduledTaskService

private const val RECURRING = "recurring"

fun Route.mottakApi(
    scheduler: Scheduler = JobTaskConfig.scheduler(),
    scheduledTaskService: ScheduledTaskService = ScheduledTaskService(),
) {
    route("api/v1") {
        post("readParseFileAndValidateTransactions") {
            val ident = AuthToken.getSaksbehandler(call)
            call.launch(Dispatchers.IO) {
                val task = JobTaskConfig.recurringReadParseFileAndValidateTransactionsTask()
                scheduler.reschedule(task.instance(RECURRING), Instant.now(), ident)
            }
            call.respond(HttpStatusCode.Accepted, "ReadAndParseFile av filer har startet, sjekk logger for status")
        }

        post("sendUtbetalingTransaksjonToOppdragZ") {
            val ident = AuthToken.getSaksbehandler(call)
            call.launch(Dispatchers.IO) {
                val task = JobTaskConfig.recurringSendUtbetalingTransaksjonToOppdragZTask()
                scheduler.reschedule(task.instance(RECURRING), Instant.now(), ident)
            }
            call.respond(HttpStatusCode.Accepted, "SendUtbetalingTransaksjonTilOppdrag har startet, sjekk logger for status")
        }

        post("sendTrekkTransaksjonToOppdragZ") {
            val ident = AuthToken.getSaksbehandler(call)
            call.launch(Dispatchers.IO) {
                val task = JobTaskConfig.recurringSendTrekkTransaksjonToOppdragZTask()
                scheduler.reschedule(task.instance(RECURRING), Instant.now(), ident)
            }
            call.respond(HttpStatusCode.Accepted, "SendTrekkTransaksjonTilOppdrag har startet, sjekk logger for status")
        }

        post("avstemming") {
            val ident = AuthToken.getSaksbehandler(call)
            val request = call.receive<AvstemmingRequest>()
            call.launch(Dispatchers.IO) {
                val task = JobTaskConfig.recurringGrensesnittAvstemmingTask()
                val requestData = Json.encodeToString(Pair(ident, request))
                scheduler.reschedule(task.instance(RECURRING), Instant.now(), requestData)
            }
            call.respond(HttpStatusCode.Accepted, "GrensesnittAvstemming har startet, sjekk logger for status")
        }

        get("jobTaskInfo") {
            call.respond(HttpStatusCode.OK, scheduledTaskService.getScheduledTaskInformation())
        }
    }
}
