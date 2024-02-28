package no.nav.sokos.spk.mottak.modell

import java.math.BigDecimal
import java.time.LocalDate

class Transaksjon(
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
    var status: String,
    var feiltekst: String
) {
    override fun toString(): String {
        return "Transaksjon(transId='$transId', gjelderId='$gjelderId', utbetalesTil='$utbetalesTil', datoAnviser=$datoAnviser, periodeFOM=$periodeFOM, periodeTOM=$periodeTOM, belopsType='$belopsType', belop=$belop, art='$art', refTransId='$refTransId', tekstKode='$tekstKode', saldo=$saldo, prioritet='$prioritet', kid='$kid', trekkansvar='$trekkansvar', grad='$grad', status='$status', feiltekst='$feiltekst')"
    }
}