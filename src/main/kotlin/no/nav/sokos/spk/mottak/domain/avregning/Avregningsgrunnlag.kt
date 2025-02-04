package no.nav.sokos.spk.mottak.domain.avregning

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Avregningsgrunnlag(
    @SerialName("OPPDRAGS-ID") val oppdragsId: String? = null,
    @SerialName("LINJE-ID") val linjeId: String? = null,
    @SerialName("TREKKVEDTAK-ID") val trekkvedtakId: String? = null,
    @SerialName("GJELDER-ID") val gjelderId: String,
    @SerialName("UTBETALES-TIL") val utbetalesTil: String,
    @SerialName("DATO-STATUS-SATT") val datoStatusSatt: String,
    @SerialName("STATUS") val status: String,
    @SerialName("BILAGSNR-SERIE") val bilagsnrSerie: String? = null,
    @SerialName("BILAGSNR") val bilagsnr: String? = null,
    @SerialName("KONTO") val konto: String? = null,
    @SerialName("FOMDATO") val fomdato: String,
    @SerialName("TOMDATO") val tomdato: String,
    @SerialName("BELOP") val belop: String,
    @SerialName("DEBET-KREDIT") val debetKredit: String,
    @SerialName("UTBETALINGS-TYPE") val utbetalingsType: String? = null,
    @SerialName("TRANS-TEKST") val transTekst: String? = null,
    @SerialName("DATO-VALUTERT") val datoValutert: String? = null,
    @SerialName("DELYTELSE-ID") val delytelseId: String? = null,
)

@Serializable
data class AvregningsgrunnlagWrapper(
    @SerialName("avregningsgrunnlag") val avregningsgrunnlag: Avregningsgrunnlag,
)
