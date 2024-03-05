package no.nav.sokos.spk.mottak.modell

import java.math.BigDecimal
import java.time.LocalDate

data class Transaction(
    val transId: String,
    val gjelderId: String,
    val utbetalesTil: String,
    val datoAnviser: LocalDate,
    val periodeFOM: LocalDate,
    val periodeTOM: LocalDate,
    val belopsType: String,
    val belop: BigDecimal,
    val art: String,
    val refTransId: String,
    val tekstKode: String,
    val saldo: BigDecimal,
    val prioritet: String,
    val kid: String,
    val trekkansvar: String,
    val grad: String,
    var status: String? = null,
    var feiltekst: String? = null
)