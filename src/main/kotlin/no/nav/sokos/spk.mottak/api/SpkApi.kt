package no.nav.sokos.spk.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.service.FileReaderService

private val logger = KotlinLogging.logger {}

fun Route.spkApi(
    fileReaderService: FileReaderService = FileReaderService()
) {
    route("api/v1") {

        get("/manuellprosessering") {
            fileReaderService.readAndParseFile()
            logger.info { "Trigger manuell prosessering av filer" }
            call.respond(HttpStatusCode.OK)
        }
    }
}