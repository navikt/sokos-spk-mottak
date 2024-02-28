package no.nav.sokos.spk.mottak.database

import java.math.BigDecimal

data class InnTransaksjon(
    val id: Int,
    val filInfoId: Int,
    val belopType: String,
    val art: String,
    val gjelderId: String,
    val status: String?,
    val utbetalesTil: String,
    val datoFom: String,
    val datoTom: String,
    val refTransId: String,
    val belop: BigDecimal,
    val tekstKode: String,
    val recType: String,
    val transId: String,
    val datoAnviser: String,
    val avsender: String,
    val saldo: BigDecimal,
    val prioritet: String,
    val kid: String,
    val trekkansvar: String,
    val grad: String,
    val behandlet: String,
    val datoOpprettet: String,
    val OpprettetAv: String,
    val datoEndret: String,
    val endretAv: String,
    val versjon: String,
    var feiltekst: String,
) {
    override fun toString(): String {
        return "InnTransaksjon(id=$id, filInfoId=$filInfoId, belopType='$belopType', art='$art', gjelderId='$gjelderId', status=$status, utbetalesTil='$utbetalesTil', datoFom='$datoFom', datoTom='$datoTom', refTransId='$refTransId', belop=$belop, tekstKode='$tekstKode', recType='$recType', transId='$transId', datoAnviser='$datoAnviser', avsender='$avsender', saldo=$saldo, prioritet='$prioritet', kid='$kid', trekkansvar='$trekkansvar', grad='$grad', behandlet='$behandlet', datoOpprettet='$datoOpprettet', OpprettetAv='$OpprettetAv', datoEndret='$datoEndret', endretAv='$endretAv', versjon='$versjon', feiltekst='$feiltekst')"
    }
}