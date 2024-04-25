package no.nav.sokos.spk.mottak.domain.record

import no.nav.sokos.spk.mottak.validator.FileStatus

data class TransaksjonRecord(
    val transId: String,
    val fnr: String,
    val utbetalesTil: String,
    val datoAnviser: String,
    val datoFom: String,
    val datoTom: String,
    val belopstype: String,
    val belop: String,
    val art: String,
    val refTransId: String,
    val tekstKode: String,
    val saldo: String,
    val prioritet: String,
    val kid: String,
    val trekkansvar: String,
    val grad: String,
    var fileStatus: FileStatus
)