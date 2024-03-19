package no.nav.sokos.spk.mottak.validator

import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.NAV
import no.nav.sokos.spk.mottak.domain.SPK

import no.nav.sokos.spk.mottak.domain.record.RecordData

object FileValidation {
    fun validateStartAndEndRecord(recordData: RecordData): FileStatusValidation {
        return when {
            recordData.startRecord.avsender != SPK -> FileStatusValidation.UGYLDIG_ANVISER
            recordData.startRecord.mottager != NAV -> FileStatusValidation.UGYLDIG_MOTTAKER
            recordData.startRecord.filType != FILETYPE_ANVISER -> FileStatusValidation.UGYLDIG_FILTYPE
            recordData.maxLopenummer >= recordData.startRecord.filLopenummer -> FileStatusValidation.FILLOPENUMMER_I_BRUK
            recordData.startRecord.filLopenummer != (recordData.maxLopenummer + 1) -> FileStatusValidation.UGYLDIG_FILLOPENUMMER
            recordData.endRecord.totalBelop != recordData.totalBelop -> FileStatusValidation.UGYLDIG_SUMBELOP
            recordData.endRecord.numberOfRecord != recordData.numberOfRecord.minus(2) -> FileStatusValidation.UGYLDIG_ANTRECORDS
            else -> FileStatusValidation.OK
        }
    }
}
