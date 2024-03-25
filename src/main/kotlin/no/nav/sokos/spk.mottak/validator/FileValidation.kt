package no.nav.sokos.spk.mottak.validator

import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.NAV
import no.nav.sokos.spk.mottak.domain.SPK

import no.nav.sokos.spk.mottak.domain.record.RecordData

object FileValidation {
    fun validateStartAndEndRecord(recordData: RecordData): FileStatus {
        return when {
            recordData.startRecord.avsender != SPK -> FileStatus.UGYLDIG_ANVISER
            recordData.startRecord.mottager != NAV -> FileStatus.UGYLDIG_MOTTAKER
            recordData.startRecord.filType != FILETYPE_ANVISER -> FileStatus.UGYLDIG_FILTYPE
            recordData.maxLopenummer?.let { max -> max >= recordData.startRecord.filLopenummer } ?: false -> FileStatus.FILLOPENUMMER_I_BRUK
            recordData.maxLopenummer?.let { max -> (max + 1) != recordData.startRecord.filLopenummer } ?: false -> FileStatus.UGYLDIG_FILLOPENUMMER
            recordData.endRecord.totalBelop != recordData.totalBelop -> FileStatus.UGYLDIG_SUMBELOP
            recordData.endRecord.numberOfRecord != recordData.innTransaksjonList.size -> FileStatus.UGYLDIG_ANTRECORDS
            else -> FileStatus.OK
        }
    }
}
