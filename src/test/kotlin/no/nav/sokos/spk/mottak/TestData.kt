package no.nav.sokos.spk.mottak

import java.time.LocalDate
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.domain.record.StartRecord

object TestData {
    fun recordDataMock(): RecordData {
        return RecordData(
            startRecord = startRecordMock(),
            endRecord = endRecordMock(),
            innTransaksjonList = MutableList(8) { innTransaksjonMock() },
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

    fun innTransaksjonMock(): InnTransaksjon {
        return InnTransaksjon(
            transId = "116684810",
            fnr = "66064900162",
            utbetalesTil = "",
            datoAnviserStr = "20240131",
            datoFomStr = "20240201",
            datoTomStr = "20240229",
            belopstype = "01",
            belopStr = "00000346900",
            art = "UFT",
            refTransId = "",
            tekstKode = "",
            saldoStr = "00000000410",
            prioritetStr = "",
            kid = "",
            trekkansvar = "",
            gradStr = ""
        )
    }
}