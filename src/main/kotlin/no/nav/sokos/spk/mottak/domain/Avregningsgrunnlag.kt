package no.nav.sokos.spk.mottak.domain

import java.time.LocalDateTime

import kotlinx.serialization.Serializable

import no.nav.sokos.spk.mottak.dto.Avregningstransaksjon
import no.nav.sokos.spk.mottak.util.Utils.toLocalDateNotBlank

@Serializable
data class Avregningsgrunnlag(
    val oppdragsId: Int,
    val linjeId: Int? = null,
    val trekkvedtakId: Int? = null,
    val gjelderId: String,
    val utbetalesTil: String,
    val datoStatusSatt: String,
    val sattStatus: String,
    val bilagsnrSerie: String,
    val bilagsnr: String,
    val konto: String,
    val fomdato: String,
    val tomdato: String,
    val belop: Int? = 0,
    val debetKredit: String,
    val utbetalingsType: String? = null,
    val transTekst: String? = null,
    val datoValutert: String? = null,
    val delytelseId: String? = null,
    val fagSystemId: String,
    val kreditorRef: String? = null,
)

@Serializable
data class AvregningsgrunnlagWrapper(
    val avregningsgrunnlag: Avregningsgrunnlag,
)

fun Avregningsgrunnlag.toAvregningsretur(avregningstransaksjon: Avregningstransaksjon): Avregningsretur =
    Avregningsretur(
        osId = oppdragsId.toString(),
        osLinjeId = linjeId?.toString(),
        trekkvedtakId = trekkvedtakId?.toString(),
        gjelderId = gjelderId,
        fnr = avregningstransaksjon.fnr,
        datoStatus = datoStatusSatt.toLocalDateNotBlank(),
        status = sattStatus,
        bilagsnrSerie = bilagsnrSerie,
        bilagsnr = bilagsnr.padStart(10, '0'),
        datoFom = fomdato.toLocalDateNotBlank(),
        datoTom = tomdato.toLocalDateNotBlank(),
        belop = belop?.times(100)?.toString() ?: "0",
        debetKredit = debetKredit,
        utbetalingtype = utbetalingsType.orEmpty(),
        transTekst = transTekst,
        transEksId = avregningstransaksjon.transEksId,
        datoAvsender = avregningstransaksjon.datoAnviser,
        utbetalesTil = utbetalesTil,
        transaksjonId = avregningstransaksjon.transaksjonId,
        datoValutering = datoValutert.orEmpty(),
        konto = konto,
        motId = delytelseId,
        personId = fagSystemId,
        kreditorRef = kreditorRef,
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = AVREGNING_LISTENER_SERVICE,
        datoEndret = LocalDateTime.now(),
        endretAv = AVREGNING_LISTENER_SERVICE,
    )

fun Avregningsgrunnlag.toAvregningsAvvik(): AvregningsgrunnlagAvvik {
    return AvregningsgrunnlagAvvik(
        oppdragsId = oppdragsId,
        linjeId = linjeId,
        trekkvedtakId = trekkvedtakId,
        gjelderId = gjelderId,
        utbetalesTil = utbetalesTil,
        datoStatusSatt = datoStatusSatt,
        status = sattStatus,
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
