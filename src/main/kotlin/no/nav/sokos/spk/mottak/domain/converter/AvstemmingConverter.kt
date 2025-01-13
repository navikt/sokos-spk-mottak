package no.nav.sokos.spk.mottak.domain.converter

import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import no.nav.sokos.spk.mottak.domain.MOT
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TransaksjonDetalj
import no.nav.sokos.spk.mottak.domain.TransaksjonOppsummering
import no.nav.sokos.spk.mottak.util.Utils.toAvstemmingPeriode
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Aksjonsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AvstemmingType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.DetaljType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Detaljdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Fortegn
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Grunnlagsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.KildeType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Periodedata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Totaldata

const val AVLEVERENDE_KOMPONENT_KODE = "SPKMOT"
const val MOTTAKENDE_KOMPONENT_KODE = "OS"

object AvstemmingConverter {
    fun default(
        fom: String,
        tom: String,
        fagomrade: String,
    ): Avstemmingsdata =
        Avstemmingsdata().apply {
            aksjon =
                Aksjonsdata().apply {
                    kildeType = KildeType.AVLEV
                    avstemmingType = AvstemmingType.GRSN
                    avleverendeKomponentKode = AVLEVERENDE_KOMPONENT_KODE
                    mottakendeKomponentKode = MOTTAKENDE_KOMPONENT_KODE
                    underkomponentKode = fagomrade
                    nokkelFom = fom
                    nokkelTom = tom
                    avleverendeAvstemmingId = generateCustomUUID()
                    brukerId = MOT
                }
        }

    fun Avstemmingsdata.startMelding(): Avstemmingsdata =
        this.copy().apply {
            aksjon = this.aksjon.copy().apply { aksjonType = AksjonType.START }
        }

    fun Avstemmingsdata.sluttMelding() =
        this.copy().apply {
            aksjon = this.aksjon.copy().apply { aksjonType = AksjonType.AVSL }
            detalj.clear()
        }

    fun Avstemmingsdata.dataMelding(oppsummeringList: List<TransaksjonOppsummering>): Avstemmingsdata {
        val godkjentList = oppsummeringList.filter { it.osStatus == 0 }
        val varselList = oppsummeringList.filter { it.osStatus in 1..4 }
        val avvistList = oppsummeringList.filter { it.transTilstandType == TRANS_TILSTAND_OPPDRAG_RETUR_FEIL }
        val manglerList = oppsummeringList.filter { it.osStatus == null && it.transTilstandType != TRANS_TILSTAND_OPPDRAG_SENDT_FEIL }

        return this.copy().apply {
            aksjon = this.aksjon.copy().apply { aksjonType = AksjonType.DATA }
            total =
                Totaldata().apply {
                    totalAntall = oppsummeringList.sumOf { it.antall }
                    totalBelop = oppsummeringList.sumOf { it.belop }
                    fortegn = Fortegn.T
                }
            periode =
                Periodedata().apply {
                    datoAvstemtFom = LocalDate.now().atStartOfDay().toAvstemmingPeriode()
                    datoAvstemtTom = LocalTime.MAX.atDate(LocalDate.now()).toAvstemmingPeriode()
                }
            grunnlag =
                Grunnlagsdata().apply {
                    godkjentAntall = godkjentList.sumOf { it.antall }
                    godkjentBelop = godkjentList.sumOf { it.belop }
                    godkjentFortegn = Fortegn.T

                    varselAntall = varselList.sumOf { it.antall }
                    varselBelop = varselList.sumOf { it.belop }
                    varselFortegn = Fortegn.T

                    avvistAntall = avvistList.sumOf { it.antall }
                    avvistBelop = avvistList.sumOf { it.belop }
                    avvistFortegn = Fortegn.T

                    manglerAntall = manglerList.sumOf { it.antall }
                    manglerBelop = manglerList.sumOf { it.belop }
                    manglerFortegn = Fortegn.T
                }
            detalj.clear()
        }
    }

    fun Avstemmingsdata.avvikMelding(transaksjonDetaljer: List<TransaksjonDetalj>): Avstemmingsdata =
        this.copy().apply {
            aksjon = this.aksjon.apply { aksjonType = AksjonType.DATA }
            detalj.addAll(
                transaksjonDetaljer.map { transaksjonDetalj ->
                    Detaljdata().apply {
                        detaljType = transaksjonDetalj.detaljType()
                        offnr = transaksjonDetalj.fnr
                        avleverendeTransaksjonNokkel = transaksjonDetalj.fagsystemId
                        meldingKode = transaksjonDetalj.feilkode
                        alvorlighetsgrad = transaksjonDetalj.osStatus
                        tekstMelding = transaksjonDetalj.feilkodeMelding
                        tidspunkt = transaksjonDetalj.datoOpprettet.format(DateTimeFormatter.ISO_DATE_TIME)
                    }
                },
            )
        }

    private fun Avstemmingsdata.copy(): Avstemmingsdata =
        Avstemmingsdata().apply {
            aksjon = this@copy.aksjon
            total = this@copy.total
            periode = this@copy.periode
            grunnlag = this@copy.grunnlag
            detalj.addAll(this@copy.detalj)
        }

    private fun Aksjonsdata.copy(): Aksjonsdata =
        Aksjonsdata().apply {
            aksjonType = this@copy.aksjonType
            kildeType = this@copy.kildeType
            avstemmingType = this@copy.avstemmingType
            avleverendeKomponentKode = this@copy.avleverendeKomponentKode
            mottakendeKomponentKode = this@copy.mottakendeKomponentKode
            underkomponentKode = this@copy.underkomponentKode
            nokkelFom = this@copy.nokkelFom
            nokkelTom = this@copy.nokkelTom
            tidspunktAvstemmingTom = this@copy.tidspunktAvstemmingTom
            avleverendeAvstemmingId = this@copy.avleverendeAvstemmingId
            brukerId = this@copy.brukerId
        }

    private fun TransaksjonDetalj.detaljType(): DetaljType =
        osStatus?.let { status ->
            when (status.toInt()) {
                0 -> throw IllegalStateException("transaksjonId: $transaksjonId, ugyldig OS status type")
                in 1..4 -> DetaljType.VARS
                else -> DetaljType.AVVI
            }
        } ?: DetaljType.MANG

    private fun generateCustomUUID(): String {
        val uuid = UUID.randomUUID().toString().replace("-", "")
        return if (uuid.length > 30) uuid.substring(0, 30) else uuid
    }
}
