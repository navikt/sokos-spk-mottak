package no.nav.sokos.spk.mottak

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.integration.models.FullmaktDTO
import java.time.LocalDate
import java.time.LocalDateTime

const val SPK_FILE_OK = "SPK_NAV_20242503_070026814_ANV_OK.txt"
const val SPK_FEIL_UGYLDIG_ANVISER = "SPK_NAV_20362503_080026814_ANV_UGYLDIG_ANVISER.txt"
const val SPK_FEIL_UGYLDIG_MOTTAKER = "SPK_NAV_20342503_080026814_ANV_UGYLDIG_MOTTAKER.txt"
const val SPK_FEIL_FILLOPENUMMER_I_BRUK = "SPK_NAV_20272503_080026814_ANV_FILLOPENUMMER_I_BRUK.txt"
const val SPK_FEIL_UGYLDIG_LOPENUMMER = "SPK_NAV_20332503_080026814_ANV_UGYLDIG_LOPENUMMER.txt"
const val SPK_FEIL_FORVENTET_FILLOPENUMMER = "SPK_NAV_20282503_080026814_ANV_FORVENTET_FILLOPENUMMER.txt"
const val SPK_FEIL_UGYLDIG_FILTYPE = "SPK_NAV_20352503_080026814_ANV_UGYLDIG_FILTYPE.txt"
const val SPK_FEIL_UGYLDIG_ANTRECORDS = "SPK_NAV_20262503_080026814_ANV_UGYLDIG_ANTRECORDS.txt"
const val SPK_FEIL_UGYLDIG_SUMBELOP = "SPK_NAV_20252503_080026814_ANV_UGYLDIG_SUMBELOP.txt"
const val SPK_FEIL_UGYLDIG_PRODDATO = "SPK_NAV_20292503_080026814_ANV_UGYLDIG_PRODDATO.txt"
const val SPK_FILE_FEIL = "SPK_NAV_20242503_080026814_ANV_FEIL.txt"
const val SPK_FEIL_UGYLDIG_START_RECTYPE = "SPK_NAV_20312503_080026814_ANV_UGYLDIG_START_RECTYPE.txt"
const val SPK_FEIL_UGYLDIG_END_RECTYPE = "SPK_NAV_20322503_080026814_ANV_UGYLDIG_END_RECTYPE.txt"
const val SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP = "SPK_NAV_20372503_080026814_ANV_UGYLDIG_TRANSAKSJONS_BELOP.txt"
const val SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE = "SPK_NAV_20302503_080026814_ANV_UGYLDIG_TRANSAKSJON_RECTYPE.txt"

object TestData {
    fun fullmakterMock(): List<FullmaktDTO> {
        return listOf(
            FullmaktDTO(
                aktorIdentGirFullmakt = "22031366171",
                aktorIdentMottarFullmakt = "07846497913",
                kodeAktorTypeGirFullmakt = "PERSON",
                kodeAktorTypeMottarFullmakt = "PERSON",
                kodeFullmaktType = "VERGE_PENGEMOT",
            ),
            FullmaktDTO(
                aktorIdentGirFullmakt = "02500260109",
                aktorIdentMottarFullmakt = "08049011297",
                kodeAktorTypeGirFullmakt = "PERSON",
                kodeAktorTypeMottarFullmakt = "PERSON",
                kodeFullmaktType = "VERGE_PENGEMOT",
            ),
        )
    }

    fun innTransaksjonMock(): InnTransaksjon {
        val systemId = PropertiesConfig.Configuration().naisAppName
        return InnTransaksjon(
            innTransaksjonId = 1,
            filInfoId = 123,
            transaksjonStatus = null,
            fnr = "22031366171",
            belopstype = BELOPTYPE_SKATTEPLIKTIG_UTBETALING,
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
