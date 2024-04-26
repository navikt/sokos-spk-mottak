package no.nav.sokos.spk.mottak.domain.record

import no.nav.sokos.spk.mottak.validator.FileStatus

data class EndRecord(
    val numberOfRecord: Int,
    val totalBelop: Long,
    var fileStatus: FileStatus
)