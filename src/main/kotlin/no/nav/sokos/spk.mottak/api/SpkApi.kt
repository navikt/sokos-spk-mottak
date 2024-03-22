package no.nav.sokos.spk.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.sokos.spk.mottak.service.FileReaderService

fun Route.spkApi(
    fileReaderService: FileReaderService = FileReaderService()
) {
    route("mottak") {

        get("fetchFiles") {
            fileReaderService.readAndParseFile()
            call.respond(HttpStatusCode.OK)
        }
    }
}