package no.nav.sokos.spk.mottak.domain.record

import no.nav.sokos.spk.mottak.domain.InnTransaksjon

data class RecordData(
    var filename: String? = null,
    val startRecord: StartRecord,
    val endRecord: EndRecord,
    val innTransaksjonList: MutableList<InnTransaksjon>,
    var maxLopenummer: Int? = 0,
    val totalBelop: Long
)