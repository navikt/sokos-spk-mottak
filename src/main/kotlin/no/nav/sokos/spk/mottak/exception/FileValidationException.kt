package no.nav.sokos.spk.mottak.exception

import no.nav.sokos.spk.mottak.validator.FileStatus

class FileValidationException(val statusCode: String, override val message: String) : Exception(message) {
    constructor(fileStatus: FileStatus) : this(fileStatus.code, fileStatus.message)
}