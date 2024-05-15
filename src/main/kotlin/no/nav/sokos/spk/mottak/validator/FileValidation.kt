package no.nav.sokos.spk.mottak.validator

import mu.KotlinLogging
import no.nav.sokos.spk.mottak.domain.FILTYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.NAV
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.exception.FilValidationException

private val logger = KotlinLogging.logger {}

object FileValidation {
    fun validateStartAndSluttRecord(recordData: RecordData) {
        when {
            recordData.startRecord.avsender != SPK -> throw FilValidationException(FilStatus.UGYLDIG_ANVISER)
            recordData.startRecord.mottager != NAV -> throw FilValidationException(FilStatus.UGYLDIG_MOTTAKER)
            recordData.startRecord.filType != FILTYPE_ANVISER -> throw FilValidationException(FilStatus.UGYLDIG_FILTYPE)
            recordData.maxLopenummer?.let { max -> max >= recordData.startRecord.filLopenummer } ?: false ->
                throw FilValidationException(FilStatus.FILLOPENUMMER_I_BRUK.code, String.format(FilStatus.FILLOPENUMMER_I_BRUK.message, "${recordData.startRecord.filLopenummer}"))

            recordData.maxLopenummer?.let { max -> (max + 1) != recordData.startRecord.filLopenummer } ?: false ->
                throw FilValidationException(FilStatus.FORVENTET_FILLOPENUMMER.code, String.format(FilStatus.FORVENTET_FILLOPENUMMER.message, "${recordData.maxLopenummer!! + 1}"))

            recordData.sluttRecord.totalBelop != recordData.totalBelop ->
                throw FilValidationException(FilStatus.UGYLDIG_SUMBELOP.code, String.format(FilStatus.UGYLDIG_SUMBELOP.message, "${recordData.sluttRecord.totalBelop}", "${recordData.totalBelop}"))

            recordData.sluttRecord.antallRecord != recordData.transaksjonRecordList.size ->
                throw FilValidationException(
                    FilStatus.UGYLDIG_ANTRECORDS.code,
                    String.format(FilStatus.UGYLDIG_ANTRECORDS.message, "${recordData.sluttRecord.antallRecord}", "${recordData.transaksjonRecordList.size}"),
                )

            else -> logger.debug { "Status på filen: ${FilStatus.OK}" }
        }
    }
}
