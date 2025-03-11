package no.nav.sokos.spk.mottak.domain

import java.time.LocalDateTime

import kotlinx.serialization.Serializable

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.dto.Avregningstransaksjon
import no.nav.sokos.spk.mottak.util.Utils.toLocalDateNotBlank
import no.nav.sokos.spk.mottak.util.Utils.toLocalDateStringOrEmpty

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

fun Avregningsgrunnlag.toAvregningsretur(avregningstransaksjon: Avregningstransaksjon): Avregningsretur =
    Avregningsretur(
        osId = oppdragsId.toString(),
        osLinjeId = linjeId?.toString(),
        trekkvedtakId = trekkvedtakId?.toString(),
        gjelderId = gjelderId,
        fnr = avregningstransaksjon.fnr,
        datoStatus = datoStatusSatt.toLocalDateNotBlank(),
        status = status,
        bilagsnrSerie = bilagsnrSerie,
        bilagsnr = bilagsnr,
        datoFom = fomdato.toLocalDateNotBlank(),
        datoTom = tomdato.toLocalDateNotBlank(),
        belop = belop.toString(),
        debetKredit = debetKredit,
        utbetalingtype = utbetalingsType,
        transTekst = transTekst,
        transEksId = avregningstransaksjon.transEksId,
        datoAvsender = avregningstransaksjon.datoAnviser,
        utbetalesTil = utbetalesTil,
        transaksjonId = avregningstransaksjon.transaksjonId,
        datoValutering = datoValutert.toLocalDateStringOrEmpty(),
        konto = konto,
        motId = delytelseId,
        personId = fagSystemId,
        kreditorRef = kreditorRef,
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = PropertiesConfig.Configuration().naisAppName,
        datoEndret = LocalDateTime.now(),
        endretAv = PropertiesConfig.Configuration().naisAppName,
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
