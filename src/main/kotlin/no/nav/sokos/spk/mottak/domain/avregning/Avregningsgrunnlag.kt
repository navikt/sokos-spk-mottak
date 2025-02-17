package no.nav.sokos.spk.mottak.domain.avregning

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
    val bilagsnrSerie: String? = null,
    val bilagsnr: String? = null,
    val konto: String? = null,
    val fomdato: String,
    val tomdato: String,
    val belop: Int,
    val debetKredit: String,
    val utbetalingsType: String? = null,
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
