package no.nav.sokos.spk.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.service.ReadAndParseFileService
import no.nav.sokos.spk.mottak.service.ValidateTransaksjonService

private val logger = KotlinLogging.logger {}

fun Route.mottakApi(
    readAndParseFileService: ReadAndParseFileService = ReadAndParseFileService(),
    validateTransaksjonService: ValidateTransaksjonService = ValidateTransaksjonService(),
) {
    route("api/v1") {
        get("filprosessering") {
            launch(Dispatchers.IO) {
                readAndParseFileService.readAndParseFile()
            }
            call.respond(HttpStatusCode.OK, "Filprosessering av filer har startet, sjekk logger for status")
        }

        get("transaksjonvalidering") {
            launch(Dispatchers.IO) {
                validateTransaksjonService.validateInnTransaksjon()
            }
            call.respond(HttpStatusCode.OK, "Transaksjonsvalidering har startet, sjekk logger for status")
        }
    }
}
