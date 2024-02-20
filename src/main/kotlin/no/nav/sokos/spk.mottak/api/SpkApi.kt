package no.nav.sokos.spk.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import no.nav.sokos.spk.mottak.service.FtpService

fun Route.spkApi(
    ftpService: FtpService = FtpService()
) {
        route("mottak") {

            get("listFiles/{directory}") {
                val directory = call.parameters["directory"].toString()
                val response = ftpService.listAllFiles(directory)
                call.respond(HttpStatusCode.OK, response)
            }
        }
}