package no.nav.sokos.spk.mottak.modell

import java.time.LocalDate

data class Transaction(
    val transId: String,
    val gjelderId: String,
    val utbetalesTil: String,
    val datoAnviserStr: String,
    var datoAnviser: LocalDate? = null,
    val periodeFomStr: String,
    var periodeFom: LocalDate? = null,
    val periodeTomStr: String,
    var periodeTom: LocalDate? = null,
    val belopsType: String,
    val belopStr: String,
    var belop: Int? = -1,
    val art: String,
    val refTransId: String,
    val tekstKode: String,
    val saldoStr: String,
    var saldo: Int? = -1,
    val prioritetStr: String,
    var prioritet: LocalDate? = null,
    val kid: String,
    val trekkansvar: String,
    val gradStr: String,
    var grad: Int? = -1,
    var status: String? = null,
    var feiltekst: String? = null
)