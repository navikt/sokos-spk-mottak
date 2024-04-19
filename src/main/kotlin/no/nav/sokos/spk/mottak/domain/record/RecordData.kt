package no.nav.sokos.spk.mottak.domain.record

import no.nav.sokos.spk.mottak.validator.FileStatus

data class RecordData(
    var filename: String? = null,
    val startRecord: StartRecord,
    val endRecord: EndRecord,
    val transaksjonRecordList: MutableList<TransaksjonRecord>,
    var maxLopenummer: Int? = 0,
    val totalBelop: Long,
    var fileStatus: FileStatus
)