package no.nav.sokos.spk.mottak.validator

import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.NAV
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.StartRecord

class FileValidation(
    private val startRecord: StartRecord,
    private val endRecord: EndRecord,
    private val numberOfRecord: Int,
    private val totalBelop: Long,
    private val maxLopenummer: Int
) {
    fun validateStartAndEndRecord(): FileStatusValidation {
        try {
            require(validateGyldigAvsender()) { FileStatusValidation.UGYLDIG_ANVISER }
            require(validateGyldigMottaker()) { FileStatusValidation.UGYLDIG_MOTTAKER }
            require(validateGyldigFiltype()) { FileStatusValidation.UGYLDIG_FILTYPE }
            require(validateFillopenummerIkkeBrukt(maxLopenummer)) { FileStatusValidation.FILLOPENUMMER_I_BRUK }
            require(validateGyldigFillopenummer(maxLopenummer)) { FileStatusValidation.UGYLDIG_FILLOPENUMMER }
            require(validateGyldigSum()) { FileStatusValidation.UGYLDIG_SUMBELOP }
            require(validateGyldigAntall()) { FileStatusValidation.UGYLDIG_ANTRECORDS }
        } catch (e: Exception) {
            logger.error("Valideringsfeil: ${e.message}")
            // TODO: Hvordan håndtere generell parsingfeil
            return FileStatusValidation.UKJENT
        }
        return FileStatusValidation.OK
    }

    private fun validateGyldigAvsender(): Boolean {
        return startRecord.avsender == SPK
    }

    private fun validateGyldigMottaker(): Boolean {
        return startRecord.mottager == NAV
    }

    private fun validateGyldigFiltype(): Boolean {
        return startRecord.filType == FILETYPE_ANVISER
    }

    private fun validateGyldigFillopenummer(maxLopenummer: Int): Boolean {
        return startRecord.filLopenummer == (maxLopenummer + 1)
    }

    private fun validateFillopenummerIkkeBrukt(maxLopenummer: Int): Boolean {
        return maxLopenummer < startRecord.filLopenummer
    }

    private fun validateGyldigSum(): Boolean {
        return endRecord.totalBelop == totalBelop
    }

    private fun validateGyldigAntall(): Boolean {
        return endRecord.numberOfRecord == numberOfRecord
    }
}
