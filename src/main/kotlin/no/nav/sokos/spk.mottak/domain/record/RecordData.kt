package no.nav.sokos.spk.mottak.domain.record

data class RecordData(
    val startRecord: StartRecord,
    val endRecord: EndRecord,
    val numberOfRecord: Int,
    val totalBelop: Long,
    val maxLopenummer: Int
)