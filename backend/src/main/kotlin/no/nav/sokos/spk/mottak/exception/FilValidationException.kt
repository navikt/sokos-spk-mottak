package no.nav.sokos.spk.mottak.exception

import no.nav.sokos.spk.mottak.domain.FilStatus

class FilValidationException(
    val statusCode: String,
    override val message: String,
    val filStatus: FilStatus,
) : Exception(message) {
    constructor(filStatus: FilStatus) : this(filStatus.code, filStatus.message, filStatus)
}
