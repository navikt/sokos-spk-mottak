package no.nav.sokos.spk.mottak.validator

import mu.KotlinLogging
import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.NAV
import no.nav.sokos.spk.mottak.domain.SPK

import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.exception.ValidationException

private val logger = KotlinLogging.logger {}

object FileValidation {
    fun validateStartAndEndRecord(recordData: RecordData) {
        when {
            recordData.startRecord.avsender != SPK -> throw ValidationException(FileStatus.UGYLDIG_ANVISER)
            recordData.startRecord.mottager != NAV -> throw ValidationException(FileStatus.UGYLDIG_MOTTAKER)
            recordData.startRecord.filType != FILETYPE_ANVISER -> throw ValidationException(FileStatus.UGYLDIG_FILTYPE)
            recordData.maxLopenummer?.let { max -> max >= recordData.startRecord.filLopenummer } ?: false ->
                throw ValidationException(FileStatus.FILLOPENUMMER_I_BRUK.code, String.format(FileStatus.FILLOPENUMMER_I_BRUK.message, "${recordData.startRecord.filLopenummer}"))

            recordData.maxLopenummer?.let { max -> (max + 1) != recordData.startRecord.filLopenummer } ?: false ->
                throw ValidationException(FileStatus.FORVENTET_FILLOPENUMMER.code, String.format(FileStatus.FORVENTET_FILLOPENUMMER.message, "${recordData.maxLopenummer!! + 1}"))

            recordData.endRecord.totalBelop != recordData.totalBelop ->
                throw ValidationException(FileStatus.UGYLDIG_SUMBELOP.code, String.format(FileStatus.UGYLDIG_SUMBELOP.message, "${recordData.endRecord.totalBelop}", "${recordData.totalBelop}"))

            recordData.endRecord.numberOfRecord != recordData.transaksjonRecordList.size ->
                throw ValidationException(FileStatus.UGYLDIG_ANTRECORDS.code, String.format(FileStatus.UGYLDIG_ANTRECORDS.message, "${recordData.endRecord.numberOfRecord}", "${recordData.transaksjonRecordList.size}"))

            else -> logger.debug { "ValidationFileStatus: ${FileStatus.OK}" }
        }
    }
}
