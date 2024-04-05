package no.nav.sokos.spk.mottak.domain.record

data class RecordData(
    var filename: String? = null,
    val startRecord: StartRecord,
    val endRecord: EndRecord,
    val transaksjonRecordList: MutableList<TransaksjonRecord>,
    var maxLopenummer: Int? = 0,
    val totalBelop: Long
)