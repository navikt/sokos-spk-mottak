package no.nav.sokos.spk.mottak.domain.record

import no.nav.sokos.spk.mottak.domain.FilStatus

data class RecordData(
    var filNavn: String? = null,
    val startRecord: StartRecord,
    val sluttRecord: SluttRecord,
    val transaksjonRecordList: MutableList<TransaksjonRecord>,
    var maxLopenummer: Int? = 0,
    val totalBelop: Long,
    var filStatus: FilStatus,
)
