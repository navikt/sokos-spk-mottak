package no.nav.sokos.spk.mottak.exception

import io.ktor.client.statement.HttpResponse

class FullmaktException(ex: Exception) : Exception(ex.message)
