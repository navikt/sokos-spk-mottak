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
import no.nav.sokos.spk.mottak.integration.FullmaktClientService
import no.nav.sokos.spk.mottak.service.FileReaderService
import no.nav.sokos.spk.mottak.service.TransaksjonValideringService

private val logger = KotlinLogging.logger {}

fun Route.mottakApi(
    fileReaderService: FileReaderService = FileReaderService(),
    fullmaktClientService: FullmaktClientService = FullmaktClientService(),
    transaksjonValideringService: TransaksjonValideringService = TransaksjonValideringService()

) {
    route("api/v1") {

        get("/manuellprosessering") {
            logger.info { "Trigger manuell prosessering av filer" }
            launch(Dispatchers.IO) {
                fileReaderService.readAndParseFile()
            }
            call.respond(HttpStatusCode.OK, "Manuell prosessering av filer er startet, sjekk logger for status")
        }

        get("fullmakter") {
            logger.info { "Hent aller fullmakter" }
            call.respond(HttpStatusCode.OK, fullmaktClientService.getFullmakt())
        }

        get("transaksjonvalidering") {
            logger.info { "Trigger manuell prosessering av filer" }
            launch(Dispatchers.IO) {
                transaksjonValideringService.validereTransaksjon()
            }
            call.respond(HttpStatusCode.OK, "Transaksjon validering er startet, sjekk logger for status")
        }
    }
}