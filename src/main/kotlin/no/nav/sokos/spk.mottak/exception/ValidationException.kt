package no.nav.sokos.spk.mottak.exception

class ValidationException(val kode: String, override val message: String): Exception(message) {}