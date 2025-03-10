package no.nav.sokos.spk.mottak.domain

import kotlinx.serialization.Serializable

@Serializable
data class Avregningsgrunnlag(
    val oppdragsId: Int,
    val linjeId: Int? = null,
    val trekkvedtakId: Int? = null,
    val gjelderId: String,
    val utbetalesTil: String,
    val datoStatusSatt: String,
    val status: String,
    val bilagsnrSerie: String,
    val bilagsnr: String,
    val konto: String,
    val fomdato: String,
    val tomdato: String,
    val belop: Int,
    val debetKredit: String,
    val utbetalingsType: String,
    val transTekst: String? = null,
    val datoValutert: String,
    val delytelseId: String? = null,
    val fagSystemId: String,
    val kreditorRef: String? = null,
)

@Serializable
data class AvregningsgrunnlagWrapper(
    val avregningsgrunnlag: Avregningsgrunnlag,
)

fun Avregningsgrunnlag.toAvregningsAvvik(): AvregningsgrunnlagAvvik {
    return AvregningsgrunnlagAvvik(
        oppdragsId = oppdragsId,
        linjeId = linjeId,
        trekkvedtakId = trekkvedtakId,
        gjelderId = gjelderId,
        utbetalesTil = utbetalesTil,
        datoStatusSatt = datoStatusSatt,
        status = status,
        bilagsnrSerie = bilagsnrSerie,
        bilagsnr = bilagsnr,
        konto = konto,
        fomdato = fomdato,
        tomdato = tomdato,
        belop = belop,
        debetKredit = debetKredit,
        utbetalingsType = utbetalingsType,
        transTekst = transTekst,
        datoValutert = datoValutert,
        delytelseId = delytelseId,
        fagSystemId = fagSystemId,
        kreditorRef = kreditorRef,
    )
}
