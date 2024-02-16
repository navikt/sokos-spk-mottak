package no.nav.sokos.spk.mottak.api

import io.ktor.server.plugins.swagger.swaggerUI
import io.ktor.server.routing.Routing

fun Routing.swaggerApi() {
    swaggerUI(path = "api/v1/docs", swaggerFile = "openapi/pets.json")
}