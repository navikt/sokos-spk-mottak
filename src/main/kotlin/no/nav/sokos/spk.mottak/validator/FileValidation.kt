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
            recordData.maxLopenummer?.let { max -> max >= recordData.startRecord.filLopenummer } ?: false -> FileStatusValidation.FILLOPENUMMER_I_BRUK
            recordData.maxLopenummer?.let { max -> (max + 1) != recordData.startRecord.filLopenummer } ?: false -> FileStatusValidation.UGYLDIG_FILLOPENUMMER
            recordData.endRecord.totalBelop != recordData.totalBelop -> FileStatusValidation.UGYLDIG_SUMBELOP
            recordData.endRecord.numberOfRecord != recordData.innTransaksjonList.size -> FileStatusValidation.UGYLDIG_ANTRECORDS
            else -> FileStatusValidation.OK
        }
    }
}
