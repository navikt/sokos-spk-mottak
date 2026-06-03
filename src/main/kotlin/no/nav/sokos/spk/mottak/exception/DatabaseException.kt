package no.nav.sokos.spk.mottak.exception

class DatabaseException : Exception {
    constructor(message: String) : super(message)
    constructor(message: String, cause: Throwable) : super(message, cause)
}
