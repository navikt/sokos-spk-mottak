package no.nav.sokos.spk.mottak.domain.converter

import no.nav.sokos.spk.mottak.domain.SPK_TSS
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.getTransTolkningOppdragKode
import no.nav.sokos.spk.mottak.util.Utils.toLocalDateString
import no.nav.sokos.spk.mottak.util.Utils.toXMLGregorianCalendar
import no.trygdeetaten.skjema.oppdrag.Attestant180
import no.trygdeetaten.skjema.oppdrag.Avstemming115
import no.trygdeetaten.skjema.oppdrag.Grad170
import no.trygdeetaten.skjema.oppdrag.Oppdrag110
import no.trygdeetaten.skjema.oppdrag.OppdragsEnhet120
import no.trygdeetaten.skjema.oppdrag.OppdragsLinje150
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private const val DEFAULT_KODE_AKSJON = "1"
private const val SATSTYOE_MND = "MND"

private const val UTBETALING_FREKVENS = SATSTYOE_MND
private const val ENHET = "4819"
private const val TYPE_ENHET = "BOS"
private const val KODE_ENDRING = "NY"
private const val BRUK_KJOREPLAN = "N"
private const val ART_UFE = "UFE"
private const val TYPE_SOKNAD = "EO"
private const val SAKSBEHANDLER_ID = "MOT" // Oppdrag system tillater maks 8 bytes
private const val KODE_KOMPONENT = "MOT"

object OppdragConverter {
    /**
     * datoOppdragGjelderFom - kan settes alltid tilbake til 1900 istedenfor datoEndret - 30 år
     * Tekstkode140 - brukes ikke lenger etter 2014, dermed blir ikke logikken implementert
     */
    fun Transaksjon.oppdrag110(): Oppdrag110 =
        Oppdrag110().apply {
            kodeAksjon = DEFAULT_KODE_AKSJON
            kodeEndring = getTransTolkningOppdragKode()
            kodeFagomraade = gyldigKombinasjon!!.fagomrade
            fagsystemId = personId.toString()
            utbetFrekvens = UTBETALING_FREKVENS
            stonadId = datoFom!!.withDayOfMonth(1).toLocalDateString()
            oppdragGjelderId = fnr
            datoOppdragGjelderFom = LocalDate.of(1900, 1, 1).toXMLGregorianCalendar()
            saksbehId = SAKSBEHANDLER_ID

            if (transTolkning == TRANS_TOLKNING_NY) {
                oppdragsEnhet120.add(
                    OppdragsEnhet120().apply {
                        enhet = ENHET
                        typeEnhet = TYPE_ENHET
                        datoEnhetFom = LocalDate.of(1900, 1, 1).toXMLGregorianCalendar()
                    },
                )
            }

            avstemming115 =
                Avstemming115().apply {
                    kodeKomponent = KODE_KOMPONENT
                    nokkelAvstemming = filInfoId.toString()
                    tidspktMelding = datoOpprettet.format(DateTimeFormatter.ISO_DATE_TIME)
                }
        }

    fun Transaksjon.oppdragsLinje150(): OppdragsLinje150 =
        OppdragsLinje150().apply {
            kodeEndringLinje = KODE_ENDRING
            delytelseId = motId
            kodeKlassifik = gyldigKombinasjon!!.osKlassifikasjon
            datoKlassifikFom = LocalDate.of(1900, 1, 1).toXMLGregorianCalendar()
            datoVedtakFom = datoFom!!.toXMLGregorianCalendar()
            datoVedtakTom = datoTom!!.toXMLGregorianCalendar()
            sats = (belop / 100).toBigDecimal()
            fradragTillegg = TfradragTillegg.T
            typeSats = SATSTYOE_MND
            skyldnerId = SPK_TSS
            brukKjoreplan = BRUK_KJOREPLAN
            saksbehId = SAKSBEHANDLER_ID
            utbetalesTilId = utbetalesTil ?: fnr
            if (art == ART_UFE) {
                typeSoknad = TYPE_SOKNAD
            }
            attestant180.addAll(listOf(Attestant180().apply { attestantId = SAKSBEHANDLER_ID }))

            grad?.let {
                grad170.addAll(
                    listOf(
                        Grad170().apply {
                            typeGrad =
                                when (art) {
                                    "ALP" -> "UTAP"
                                    "AFP" -> "AFPG"
                                    "UFO" -> "UFOR"
                                    "UFT" -> "UFOR"
                                    "U67" -> "UFOR"
                                    "UFE" -> "UFOR"
                                    else -> throw UnsupportedOperationException("Ukjent ART")
                                }
                            grad = it.toBigInteger()
                        },
                    ),
                )
            }
        }
}
