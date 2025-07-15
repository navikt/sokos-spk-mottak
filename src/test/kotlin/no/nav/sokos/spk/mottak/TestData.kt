package no.nav.sokos.spk.mottak

import java.time.LocalDate
import java.time.LocalDateTime

import no.nav.pdl.enums.IdentGruppe
import no.nav.pdl.hentidenterbolk.HentIdenterBolkResult
import no.nav.pdl.hentidenterbolk.IdentInformasjon
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.GyldigKombinasjon
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY_EKSIST
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus
import no.nav.sokos.spk.mottak.domain.VALIDATE_TRANSAKSJON_SERVICE

const val SPK_OK = "P611.ANV.NAV.HUB.SPK.L000034.D240104.T003017_OK.txt"
const val SPK_FEIL_UGYLDIG_ANVISER = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_ANVISER.txt"
const val SPK_FEIL_UGYLDIG_MOTTAKER = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_MOTTAKER.txt"
const val SPK_FEIL_FILLOPENUMMER_I_BRUK = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_FILLOPENUMMER_I_BRUK.txt"
const val SPK_FEIL_UGYLDIG_LOPENUMMER = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_LOPENUMMER.txt"
const val SPK_FEIL_FORVENTET_FILLOPENUMMER = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_FORVENTET_FILLOPENUMMER.txt"
const val SPK_FEIL_UGYLDIG_FILTYPE = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_FILTYPE.txt"
const val SPK_FEIL_UGYLDIG_ANTRECORDS = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_ANTRECORDS.txt"
const val SPK_FEIL_UGYLDIG_SUMBELOP = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_SUMBELOP.txt"
const val SPK_FEIL_UGYLDIG_PRODDATO = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_PRODDATO.txt"
const val SPK_FILE_FEIL = "P611.ANV.NAV.HUB.SPK.L000035.D240104.T003017_FEIL.txt"
const val SPK_FEIL_UGYLDIG_START_RECTYPE = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_START_RECTYPE.txt"
const val SPK_FEIL_UGYLDIG_END_RECTYPE = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_END_RECTYPE.txt"
const val SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_TRANSAKSJONS_BELOP.txt"
const val SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE = "P611.ANV.NAV.HUB.SPK.L003919.D240104.T003017_UGYLDIG_TRANSAKSJON_RECTYPE.txt"

object TestData {
    fun transaksjonMock(): Transaksjon =
        Transaksjon(
            transaksjonId = 36799719,
            filInfoId = 123,
            transaksjonStatus = TransaksjonStatus.OK.code,
            personId = 1,
            belopstype = BELOPSTYPE_SKATTEPLIKTIG_UTBETALING,
            art = "UFT",
            anviser = SPK,
            fnr = "22031366171",
            utbetalesTil = null,
            osId = null,
            osLinjeId = null,
            datoFom = LocalDate.of(2024, 1, 1),
            datoTom = LocalDate.of(2024, 1, 31),
            datoAnviser = LocalDate.of(2024, 1, 1),
            datoPersonFom = LocalDate.of(2000, 1, 1),
            datoReakFom = null,
            belop = 1000,
            refTransId = null,
            tekstkode = null,
            rectype = RECTYPE_TRANSAKSJONSRECORD,
            transEksId = "112052188",
            transTolkning = TRANS_TOLKNING_NY_EKSIST,
            sendtTilOppdrag = "0",
            trekkvedtakId = null,
            fnrEndret = "0",
            motId = "36799719",
            osStatus = null,
            datoOpprettet = LocalDateTime.now(),
            opprettetAv = VALIDATE_TRANSAKSJON_SERVICE,
            datoEndret = LocalDateTime.now(),
            endretAv = VALIDATE_TRANSAKSJON_SERVICE,
            versjon = 1,
            transTilstandType = TRANS_TILSTAND_OPPRETTET,
            grad = 100,
            gyldigKombinasjon = GyldigKombinasjon(fagomrade = "PENSPK", osKlassifikasjon = "PENSPKALD01"),
        )

    fun hentIdenterBolkResultMock(
        fnr: String,
        oldFnr: String? = null,
    ): HentIdenterBolkResult {
        val identer =
            mutableListOf(
                IdentInformasjon(
                    ident = fnr,
                    historisk = false,
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                ),
            )

        oldFnr?.let {
            identer.add(
                IdentInformasjon(
                    ident = it,
                    historisk = true,
                    gruppe = IdentGruppe.FOLKEREGISTERIDENT,
                ),
            )
        }

        return HentIdenterBolkResult(
            ident = fnr,
            identer = identer,
        )
    }

    val FNR_LIST =
        listOf(
            "66043800214",
            "55044100206",
            "71032900202",
            "70033500228",
            "41113600223",
            "43103800243",
            "66064900162",
            "59063200225",
            "43084200249",
            "43084200248",
            "58052700262",
        )
}
