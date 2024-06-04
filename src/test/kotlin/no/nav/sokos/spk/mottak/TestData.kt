package no.nav.sokos.spk.mottak

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.SPK
import java.time.LocalDate
import java.time.LocalDateTime

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
    fun innTransaksjonMock(): InnTransaksjon {
        val systemId = PropertiesConfig.Configuration().naisAppName
        return InnTransaksjon(
            innTransaksjonId = 1,
            filInfoId = 123,
            transaksjonStatus = null,
            fnr = "22031366171",
            belopstype = BELOPSTYPE_SKATTEPLIKTIG_UTBETALING,
            art = "UFT",
            avsender = SPK,
            utbetalesTil = null,
            datoFomStr = "20240201",
            datoTomStr = "20240229",
            datoAnviserStr = "20240131",
            belopStr = "00000346900",
            refTransId = null,
            tekstkode = null,
            rectype = RECTYPE_TRANSAKSJONSRECORD,
            transId = "112052188",
            datoFom = LocalDate.of(2024, 2, 1),
            datoTom = LocalDate.of(2024, 2, 29),
            datoAnviser = LocalDate.of(2024, 1, 31),
            belop = 346900,
            behandlet = "N",
            datoOpprettet = LocalDateTime.now(),
            opprettetAv = systemId,
            datoEndret = LocalDateTime.now(),
            endretAv = systemId,
            versjon = 1,
            grad = null,
            gradStr = null,
            personId = 12345,
        )
    }
}
