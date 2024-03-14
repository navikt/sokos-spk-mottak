package no.nav.sokos.spk.mottak.validator

import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.modell.EndRecord
import no.nav.sokos.spk.mottak.modell.StartRecord
import java.math.BigDecimal

class ValidatorSpkFile(
    private val startRecord: StartRecord,
    private val endRecord: EndRecord,
    private val numberOfRecord: Int,
    private val totalBelop: Long,
    private val maxLopenummer: Int
) {
    fun validateStartAndEndRecord(): ValidationFileStatus {
        try {
            require(validateGyldigAvsender()) { ValidationFileStatus.UGYLDIG_ANVISER }
            require(validateGyldigMottaker()) { ValidationFileStatus.UGYLDIG_MOTTAKER }
            require(validateGyldigFiltype()) { ValidationFileStatus.UGYLDIG_FILTYPE }
            require(validateFillopenummerIkkeBrukt(maxLopenummer)) { ValidationFileStatus.FILLOPENUMMER_I_BRUK }
            require(validateGyldigFillopenummer(maxLopenummer)) { ValidationFileStatus.UGYLDIG_FILLOPENUMMER }
            require(validateGyldigSum()) { ValidationFileStatus.UGYLDIG_SUMBELOP }
            require(validateGyldigAntall()) { ValidationFileStatus.UGYLDIG_ANTRECORDS }
        } catch (e: Exception) {
            logger.error("Valideringsfeil: ${e.message}")
            // TODO: Hvordan håndtere generell parsingfeil
            return ValidationFileStatus.UKJENT
        }
        return ValidationFileStatus.OK
    }

    private fun validateGyldigAvsender(): Boolean {
        return startRecord.avsender == "SPK"
    }

    private fun validateGyldigMottaker(): Boolean {
        return startRecord.mottager == "NAV"
    }

    private fun validateGyldigFiltype(): Boolean {
        return startRecord.filType == "ANV"
    }

    private fun validateGyldigFillopenummer(maxLopenummer: Int): Boolean {
        println("validateGyldigFillopenummer:maxLopenummer: $maxLopenummer")
        return startRecord.filLopenummer == (maxLopenummer + 1)
    }

    private fun validateFillopenummerIkkeBrukt(maxLopenummer: Int): Boolean {
        println("validateFillopenummerIkkeBrukt:maxLopenummer: $maxLopenummer")
        return maxLopenummer < startRecord.filLopenummer
    }

    private fun validateGyldigSum(): Boolean {
        return endRecord.totalBelop == totalBelop
    }

    private fun validateGyldigAntall(): Boolean {
        return endRecord.numberOfRecord == numberOfRecord
    }
}
