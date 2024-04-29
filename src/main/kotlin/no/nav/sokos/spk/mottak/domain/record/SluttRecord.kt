package no.nav.sokos.spk.mottak.domain.record

import no.nav.sokos.spk.mottak.domain.FilStatus

data class SluttRecord(
    val antallRecord: Int,
    val totalBelop: Long,
    var filStatus: FilStatus
)