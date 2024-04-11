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
import no.nav.sokos.spk.mottak.service.FileReaderService
import no.nav.sokos.spk.mottak.service.client.FullmaktClientService

private val logger = KotlinLogging.logger {}

fun Route.mottakApi(
    fileReaderService: FileReaderService = FileReaderService(),
    fullmaktClientService: FullmaktClientService = FullmaktClientService()

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
            call.respond(HttpStatusCode.OK, fullmaktClientService.getFullMakter())
        }
    }
}