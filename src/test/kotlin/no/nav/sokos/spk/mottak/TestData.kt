package no.nav.sokos.spk.mottak

import java.time.LocalDate
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
import no.nav.sokos.spk.mottak.integration.models.FullmaktDTO
import no.nav.sokos.spk.mottak.validator.FileStatus

const val SPK_FILE_OK = "SPK_NAV_20242503_070026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_ANVISER = "SPK_NAV_20362503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_MOTTAKER = "SPK_NAV_20342503_080026814_ANV.txt"
const val SPK_FEIL_FILLOPENUMMER_I_BRUK = "SPK_NAV_20272503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_LOPENUMMER = "SPK_NAV_20332503_080026814_ANV.txt"
const val SPK_FEIL_FORVENTET_FILLOPENUMMER = "SPK_NAV_20282503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_FILTYPE = "SPK_NAV_20352503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_ANTRECORDS = "SPK_NAV_20262503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_SUMBELOP = "SPK_NAV_20252503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_PRODDATO = "SPK_NAV_20292503_080026814_ANV.txt"
const val SPK_FILE_FEIL = "SPK_NAV_20242503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_START_RECTYPE = "SPK_NAV_20312503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_END_RECTYPE = "SPK_NAV_20322503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP = "SPK_NAV_20372503_080026814_ANV.txt"
const val SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE = "SPK_NAV_20302503_080026814_ANV.txt"

object TestData {

    fun recordDataMock(): RecordData {
        return RecordData(
            startRecord = startRecordMock(),
            endRecord = endRecordMock(),
            transaksjonRecordList = MutableList(8) { transaksjonRecordMock() },
            totalBelop = 2775200L,
            maxLopenummer = 122,
            fileStatus = FileStatus.OK
        )
    }

    fun startRecordMock(): StartRecord {
        return StartRecord(
            avsender = "SPK",
            mottager = "NAV",
            filLopenummer = 123,
            filType = "ANV",
            produsertDato = LocalDate.now(),
            beskrivelse = "ANVISNINGSFIL",
            rawRecord = "01SPK        NAV        000034ANV20240131ANVISNINGSFIL                      00",
            fileStatus = FileStatus.OK
        )
    }

    fun endRecordMock(): EndRecord {
        return EndRecord(
            numberOfRecord = 8,
            totalBelop = 2775200,
            fileStatus = FileStatus.OK
        )
    }

    fun transaksjonRecordMock(): TransaksjonRecord {
        return TransaksjonRecord(
            transId = "116684810",
            fnr = "66064900162",
            utbetalesTil = "",
            datoAnviser = "20240131",
            datoFom = "20240201",
            datoTom = "20240229",
            belopstype = "01",
            belop = "00000346900",
            art = "UFT",
            refTransId = "",
            tekstKode = "",
            saldo = "00000000410",
            prioritet = "",
            kid = "",
            trekkansvar = "",
            grad = "",
            fileStatus = FileStatus.OK
        )
    }

    fun fullmakterMock(): List<FullmaktDTO> {
        return listOf(
            FullmaktDTO(
                aktorIdentGirFullmakt = "22031366171",
                aktorIdentMottarFullmakt = "07846497913",
                kodeAktorTypeGirFullmakt = "PERSON",
                kodeAktorTypeMottarFullmakt = "PERSON",
                kodeFullmaktType = "VERGE_PENGEMOT"
            ),
            FullmaktDTO(
                aktorIdentGirFullmakt = "02500260109",
                aktorIdentMottarFullmakt = "08049011297",
                kodeAktorTypeGirFullmakt = "PERSON",
                kodeAktorTypeMottarFullmakt = "PERSON",
                kodeFullmaktType = "VERGE_PENGEMOT"
            )
        )
    }
}