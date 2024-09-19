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
import no.nav.sokos.spk.mottak.service.SendTrekkTransaksjonToOppdragService
import no.nav.sokos.spk.mottak.service.SendUtbetalingTransaksjonToOppdragService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService
import no.nav.sokos.spk.mottak.service.WriteToFileService

fun Route.mottakApi(
    readAndParseFileService: ReadAndParseFileService = ReadAndParseFileService(),
    validateTransaksjonService: ValidateTransaksjonService = ValidateTransaksjonService(),
    writeToFileService: WriteToFileService = WriteToFileService(),
    sendUtbetalingTransaksjonToOppdragService: SendUtbetalingTransaksjonToOppdragService = SendUtbetalingTransaksjonToOppdragService(),
    sendTrekkTransaksjonToOppdragService: SendTrekkTransaksjonToOppdragService = SendTrekkTransaksjonToOppdragService(),
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

        get("writeToFile") {
            launch(Dispatchers.IO) {
                writeToFileService.writeReturnFile()
            }
            call.respond(HttpStatusCode.OK, "WriteToFileService har startet, sjekk logger for status")
        }

        get("sendUtbetalingTransaksjonTilOppdrag") {
            launch(Dispatchers.IO) {
                sendUtbetalingTransaksjonToOppdragService.fetchUtbetalingTransaksjonAndSendToOppdrag()
            }
            call.respond(HttpStatusCode.OK, "SendUtbetalingTransaksjonTilOppdrag har startet, sjekk logger for status")
        }

        get("sendTrekkTransaksjonTilOppdrag") {
            launch(Dispatchers.IO) {
                sendTrekkTransaksjonToOppdragService.fetchTrekkTransaksjonAndSendToOppdrag()
            }
            call.respond(HttpStatusCode.OK, "SendTrekkTransaksjonTilOppdrag har startet, sjekk logger for status")
        }
    }
}
