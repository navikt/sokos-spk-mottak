package no.nav.sokos.spk.mottak

import java.time.LocalDate
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
import no.nav.sokos.spk.mottak.integration.models.FullmaktDTO

const val SPK_FILE_OK = "SPK_NAV_20242503_070026814_INL.txt"
const val SPK_FILE_FEIL = "SPK_NAV_20242503_080026814_INL.txt"

object TestData {

    fun recordDataMock(): RecordData {
        return RecordData(
            startRecord = startRecordMock(),
            endRecord = endRecordMock(),
            transaksjonRecordList = MutableList(8) { transaksjonRecordnMock() },
            totalBelop = 2775200L,
            maxLopenummer = 122
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
            rawRecord = "01SPK        NAV        000034ANV20240131ANVISNINGSFIL                      00"
        )
    }

    fun endRecordMock(): EndRecord {
        return EndRecord(
            numberOfRecord = 8,
            totalBelop = 2775200
        )
    }

    fun transaksjonRecordnMock(): TransaksjonRecord {
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
            grad = ""
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