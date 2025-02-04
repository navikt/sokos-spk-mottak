package no.nav.sokos.spk.mottak.domain.converter

import java.time.LocalDateTime

import kotlinx.serialization.json.Json

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.avregning.Avregningsgrunnlag
import no.nav.sokos.spk.mottak.domain.avregning.Avregningsretur
import no.nav.sokos.spk.mottak.dto.Avregningstransaksjon
import no.nav.sokos.spk.mottak.util.Utils.toLocalDate

object AvregningConverter {
    private val json = Json { ignoreUnknownKeys = true }

    val systemId = PropertiesConfig.Configuration().naisAppName

    fun Avregningsgrunnlag.avregningsretur(avregningstransaksjon: Avregningstransaksjon): Avregningsretur =
        Avregningsretur(
            osId = oppdragsId,
            osLinjeId = linjeId,
            trekkvedtakId = trekkvedtakId,
            gjelderId = gjelderId,
            fnr = avregningstransaksjon.fnr,
            datoStatus = datoStatusSatt.toLocalDate()!!,
            status = status,
            bilagsNrSerie = bilagsnrSerie,
            bilagsNr = bilagsnr,
            datoFom = fomdato.toLocalDate()!!,
            datoTom = tomdato.toLocalDate()!!,
            belop = belop,
            debetKredit = debetKredit,
            utbetalingType = utbetalingsType,
            transaksjonTekst = transTekst,
            transEksId = avregningstransaksjon.transEksId,
            datoAvsender = avregningstransaksjon.datoAnviser,
            utbetalesTil = utbetalesTil,
            statusTekst = null,
            transaksjonId = avregningstransaksjon.transaksjonId,
            datoValutering = datoValutert,
            konto = konto,
            motId = delytelseId,
            datoOpprettet = LocalDateTime.now(),
            opprettetAv = systemId,
            datoEndret = LocalDateTime.now(),
            endretAv = systemId,
        )
}
