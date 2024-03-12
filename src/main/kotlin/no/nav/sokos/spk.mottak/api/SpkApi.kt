package no.nav.sokos.spk.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.sokos.spk.mottak.service.FileLoaderService

fun Route.spkApi(
    fileLoaderService: FileLoaderService = FileLoaderService()
) {
    route("mottak") {

        get("fetchFiles") {
            fileLoaderService.parseFiles()
            call.respond(HttpStatusCode.OK)
        }
    }
}