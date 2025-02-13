package no.nav.sokos.spk.mottak.domain.converter

import java.time.LocalDateTime

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.avregning.Avregningsgrunnlag
import no.nav.sokos.spk.mottak.domain.avregning.Avregningsretur
import no.nav.sokos.spk.mottak.dto.Avregningstransaksjon
import no.nav.sokos.spk.mottak.util.Utils.toLocalDate
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
            // how to handle a null date?
            datoStatus = datoStatusSatt.toLocalDate()!!,
            status = status,
            bilagsNrSerie = bilagsnrSerie,
            bilagsNr = bilagsnr,
            // how to handle a null date?
            datoFom = fomdato.toLocalDate()!!,
            // how to handle a null date?
            datoTom = tomdato.toLocalDate()!!,
            belop = belop.toString(),
            debetKredit = debetKredit,
            utbetalingType = utbetalingsType,
            transaksjonTekst = transTekst,
            transEksId = avregningstransaksjon.transEksId,
            datoAvsender = avregningstransaksjon.datoAnviser,
            utbetalesTil = utbetalesTil,
            statusTekst = null,
            returtypeKode = returType,
            transaksjonId = avregningstransaksjon.transaksjonId,
            //  will null or '0' represents a non-existing datoValutering?
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
