package no.nav.sokos.spk.mottak.exception

import io.ktor.client.statement.HttpResponse

class FullmaktException(val statusCode: String, override val message: String) : Exception(message)
