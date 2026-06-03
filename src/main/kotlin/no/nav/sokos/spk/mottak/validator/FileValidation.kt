package no.nav.sokos.spk.mottak.validator

import mu.KotlinLogging

import no.nav.sokos.spk.mottak.domain.FILTYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FilStatus.FILLOPENUMMER_I_BRUK
import no.nav.sokos.spk.mottak.domain.FilStatus.FORVENTET_FILLOPENUMMER
import no.nav.sokos.spk.mottak.domain.FilStatus.OK
import no.nav.sokos.spk.mottak.domain.FilStatus.UGYLDIG_ANTRECORDS
import no.nav.sokos.spk.mottak.domain.FilStatus.UGYLDIG_ANVISER
import no.nav.sokos.spk.mottak.domain.FilStatus.UGYLDIG_FILTYPE
import no.nav.sokos.spk.mottak.domain.FilStatus.UGYLDIG_MOTTAKER
import no.nav.sokos.spk.mottak.domain.FilStatus.UGYLDIG_SUMBELOP
import no.nav.sokos.spk.mottak.domain.NAV
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.exception.FilValidationException

private val logger = KotlinLogging.logger {}

object FileValidation {
    fun validateStartAndSluttRecord(recordData: RecordData) {
        when {
            recordData.startRecord.avsender != SPK -> throw FilValidationException(UGYLDIG_ANVISER)
            recordData.startRecord.mottager != NAV -> throw FilValidationException(UGYLDIG_MOTTAKER)
            recordData.startRecord.filType != FILTYPE_ANVISER -> throw FilValidationException(UGYLDIG_FILTYPE)
            recordData.maxLopenummer?.let { max -> max >= recordData.startRecord.filLopenummer.toInt() } ?: false ->
                throw FilValidationException(FILLOPENUMMER_I_BRUK.code, String.format(FILLOPENUMMER_I_BRUK.message, recordData.startRecord.filLopenummer), FILLOPENUMMER_I_BRUK)

            recordData.maxLopenummer?.let { max -> (max + 1) != recordData.startRecord.filLopenummer.toInt() } ?: false ->
                throw FilValidationException(
                    FORVENTET_FILLOPENUMMER.code,
                    String.format(FORVENTET_FILLOPENUMMER.message, "${recordData.maxLopenummer!! + 1}"),
                    FORVENTET_FILLOPENUMMER,
                )

            recordData.sluttRecord.totalBelop != recordData.totalBelop ->
                throw FilValidationException(
                    UGYLDIG_SUMBELOP.code,
                    String.format(UGYLDIG_SUMBELOP.message, "${recordData.sluttRecord.totalBelop}", "${recordData.totalBelop}"),
                    UGYLDIG_SUMBELOP,
                )

            recordData.sluttRecord.antallRecord != (recordData.transaksjonRecordList.size + 2) ->
                throw FilValidationException(
                    UGYLDIG_ANTRECORDS.code,
                    String.format(UGYLDIG_ANTRECORDS.message, "${recordData.sluttRecord.antallRecord}", "${recordData.transaksjonRecordList.size + 2}"),
                    UGYLDIG_ANTRECORDS,
                )

            else -> logger.debug { "Status på filen: $OK" }
        }
    }
}
