package no.nav.sokos.spk.mottak.exception

class ValidationException(val statusCode: String, override val message: String): Exception(message) {}