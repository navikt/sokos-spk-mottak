package no.nav.sokos.spk.mottak.api

import java.time.Instant

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

import com.github.kagkarlsson.scheduler.Scheduler
import io.ktor.http.HttpStatusCode
import io.ktor.server.auth.authenticate
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.api.model.AvstemmingRequest
import no.nav.sokos.spk.mottak.config.AUTHENTICATION_JWT
import no.nav.sokos.spk.mottak.config.JobTaskConfig
import no.nav.sokos.spk.mottak.security.AuthorizationGuard.isM2mToken
import no.nav.sokos.spk.mottak.security.AuthorizationGuard.isOboToken
import no.nav.sokos.spk.mottak.security.AuthorizationGuard.requireRole
import no.nav.sokos.spk.mottak.security.AuthorizationGuard.requireScope
import no.nav.sokos.spk.mottak.security.NavIdentClaim.getSaksbehandler
import no.nav.sokos.spk.mottak.security.Role
import no.nav.sokos.spk.mottak.security.Scope
import no.nav.sokos.spk.mottak.service.LeveAttestService
import no.nav.sokos.spk.mottak.service.ScheduledTaskService
import no.nav.sokos.spk.mottak.validator.validateDateQueryParameter

private val logger = KotlinLogging.logger {}
private const val RECURRING = "recurring"
const val API_BASE_PATH = "/api/v1"

fun Route.mottakApi(
    scheduler: Scheduler = JobTaskConfig.scheduler(),
    scheduledTaskService: ScheduledTaskService = ScheduledTaskService(),
    leveAttestService: LeveAttestService = LeveAttestService(),
) {
    route(API_BASE_PATH) {
        authenticate(AUTHENTICATION_JWT) {
            post("readParseFileAndValidateTransactions") {
                val tokenType = if (call.isOboToken()) "OBO" else "M2M"
                logger.info { "readParseFileAndValidateTransactions called with $tokenType token" }

                if (!call.requireScope(Scope.SPK_MOTTAK_ADMIN.value)) return@post
                val ident = call.getSaksbehandler()
                call.launch(Dispatchers.IO) {
                    val task = JobTaskConfig.recurringReadParseFileAndValidateTransactionsTask()
                    scheduler.reschedule(task.instance(RECURRING), Instant.now(), ident)
                }
                call.respond(HttpStatusCode.Accepted, "ReadAndParseFile av filer har startet, sjekk logger for status")
            }

            post("sendUtbetalingTransaksjonToOppdragZ") {
                if (!call.requireScope(Scope.SPK_MOTTAK_ADMIN.value)) return@post
                val ident = call.getSaksbehandler()
                call.launch(Dispatchers.IO) {
                    val task = JobTaskConfig.recurringSendUtbetalingTransaksjonToOppdragZTask()
                    scheduler.reschedule(task.instance(RECURRING), Instant.now(), ident)
                }
                call.respond(HttpStatusCode.Accepted, "SendUtbetalingTransaksjonTilOppdrag har startet, sjekk logger for status")
            }

            post("sendTrekkTransaksjonToOppdragZ") {
                if (!call.requireScope(Scope.SPK_MOTTAK_ADMIN.value)) return@post
                val ident = call.getSaksbehandler()
                call.launch(Dispatchers.IO) {
                    val task = JobTaskConfig.recurringSendTrekkTransaksjonToOppdragZTask()
                    scheduler.reschedule(task.instance(RECURRING), Instant.now(), ident)
                }
                call.respond(HttpStatusCode.Accepted, "SendTrekkTransaksjonTilOppdrag har startet, sjekk logger for status")
            }

            post("avstemming") {
                if (!call.requireScope(Scope.SPK_MOTTAK_ADMIN.value)) return@post
                val ident = call.getSaksbehandler()
                val request = call.receive<AvstemmingRequest>()
                call.launch(Dispatchers.IO) {
                    val task = JobTaskConfig.recurringGrensesnittAvstemmingTask()
                    val requestData = Json.encodeToString(Pair(ident, request))
                    scheduler.reschedule(task.instance(RECURRING), Instant.now(), requestData)
                }
                call.respond(HttpStatusCode.Accepted, "GrensesnittAvstemming har startet, sjekk logger for status")
            }

            post("writeAvregningsreturFile") {
                if (!call.requireScope(Scope.SPK_MOTTAK_ADMIN.value)) return@post
                val ident = call.getSaksbehandler()
                call.launch(Dispatchers.IO) {
                    val task = JobTaskConfig.recurringWriteAvregningsreturFileTask()
                    scheduler.reschedule(task.instance(RECURRING), Instant.now(), ident)
                }
                call.respond(HttpStatusCode.Accepted, "WriteAvregningsreturFile har startet, sjekk logger for status")
            }

            get("jobTaskInfo") {
                if (!call.requireScope(Scope.SPK_MOTTAK_ADMIN.value)) return@get
                call.respond(HttpStatusCode.OK, scheduledTaskService.getScheduledTaskInformation())
            }

            get("leveattester/{datoFom}") {
                if (!call.requireRole(Role.LEVEATTESTER_READ.value)) return@get

                val tokenType = if (call.isM2mToken()) "M2M" else "OBO"
                logger.info { "leveattester called with $tokenType token" }

                call.respond(
                    leveAttestService.getLeveAttester(call.pathParameters["datoFom"].orEmpty().validateDateQueryParameter()),
                )
            }
        }
    }
}
