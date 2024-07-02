package no.nav.sokos.spk.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import no.nav.sokos.spk.mottak.service.ReadAndParseFileService
import no.nav.sokos.spk.mottak.service.SendTrekkTransaksjonTilOppdragService
import no.nav.sokos.spk.mottak.service.SendUtbetalingTransaksjonTilOppdragService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService

fun Route.mottakApi(
    readAndParseFileService: ReadAndParseFileService = ReadAndParseFileService(),
    validateTransaksjonService: ValidateTransaksjonService = ValidateTransaksjonService(),
    sendUtbetalingTransaksjonTilOppdragService: SendUtbetalingTransaksjonTilOppdragService = SendUtbetalingTransaksjonTilOppdragService(),
    sendTrekkTransaksjonTilOppdragService: SendTrekkTransaksjonTilOppdragService = SendTrekkTransaksjonTilOppdragService(),
) {
    route("api/v1") {
        get("readAndParseFile") {
            launch(Dispatchers.IO) {
                readAndParseFileService.readAndParseFile()
            }
            call.respond(HttpStatusCode.OK, "ReadAndParseFile av filer har startet, sjekk logger for status")
        }

        get("validateTransaksjon") {
            launch(Dispatchers.IO) {
                validateTransaksjonService.validateInnTransaksjon()
            }
            call.respond(HttpStatusCode.OK, "ValidateTransaksjon har startet, sjekk logger for status")
        }

        get("sendUtbetalingTransaksjonTilOppdrag") {
            launch(Dispatchers.IO) {
                sendUtbetalingTransaksjonTilOppdragService.hentUtbetalingTransaksjonOgSendTilOppdrag()
            }
            call.respond(HttpStatusCode.OK, "SendUtbetalingTransaksjonTilOppdrag har startet, sjekk logger for status")
        }

        get("sendTrekkTransaksjonTilOppdrag") {
            launch(Dispatchers.IO) {
                sendTrekkTransaksjonTilOppdragService.hentTrekkTransaksjonOgSendTilOppdrag()
            }
            call.respond(HttpStatusCode.OK, "SendTrekkTransaksjonTilOppdrag har startet, sjekk logger for status")
        }
    }
}
