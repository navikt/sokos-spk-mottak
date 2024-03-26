package no.nav.sokos.spk.mottak.domain.record

data class InnTransaksjon(
    val transId: String,
    val fnr: String,
    val utbetalesTil: String,
    val datoAnviserStr: String,
    val datoFomStr: String,
    val datoTomStr: String,
    val belopstype: String,
    val belopStr: String,
    val art: String,
    val refTransId: String,
    val tekstKode: String,
    val saldoStr: String,
    val prioritetStr: String,
    val kid: String,
    val trekkansvar: String,
    val gradStr: String
)