package no.nav.sokos.spk.mottak.domain.converter

import java.time.LocalDateTime

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.Avregningsgrunnlag
import no.nav.sokos.spk.mottak.domain.Avregningsretur
import no.nav.sokos.spk.mottak.dto.Avregningstransaksjon
import no.nav.sokos.spk.mottak.util.Utils.toLocalDateNotBlank
import no.nav.sokos.spk.mottak.util.Utils.toLocalDateStringOrEmpty

object AvregningConverter {
    val systemId = PropertiesConfig.Configuration().naisAppName

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
            opprettetAv = systemId,
            datoEndret = LocalDateTime.now(),
            endretAv = systemId,
        )
}
