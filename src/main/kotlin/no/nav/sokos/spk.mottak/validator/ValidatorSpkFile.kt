package no.nav.sokos.spk.mottak.validator

import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.modell.EndRecord
import no.nav.sokos.spk.mottak.modell.StartRecord
import java.math.BigDecimal

class ValidatorSpkFile(
    private val startRecord: StartRecord,
    private val endRecord: EndRecord,
    private val numberOfRecord: Int,
    private val totalBelop: BigDecimal
) {
    fun validateStartAndEndRecord(): ValidationFileStatus {
        try {
            require(validateGyldigAvsender()) { ValidationFileStatus.UGYLDIG_ANVISER }
            require(validateGyldigMottaker()) { ValidationFileStatus.UGYLDIG_MOTTAKER }
            require(validateGyldigFiltype()) { ValidationFileStatus.UGYLDIG_FILTYPE }
            require(validateFillopenummerIkkeBrukt()) { ValidationFileStatus.FILLOPENUMMER_I_BRUK }
            require(validateGyldigFillopenummer(hentForrigeLopenummer())) { ValidationFileStatus.UGYLDIG_FILLOPENUMMER }
            require(validateGyldigSum()) { ValidationFileStatus.UGYLDIG_SUMBELOP }
            require(validateGyldigAntall()) { ValidationFileStatus.UGYLDIG_ANTRECORDS }
        } catch (e: Exception) {
            logger.error("Valideringsfeil: ${e.message}")
            // TODO: Hvordan håndtere generell parsingfeil
            return ValidationFileStatus.UKJENT
        }
        return ValidationFileStatus.OK
    }

    // TODO hente forrige løpenummer i T_LOPENR
    private fun hentForrigeLopenummer() = 1

    private fun validateGyldigAvsender(): Boolean {
        return startRecord.avsender == "SPK"
    }

    private fun validateGyldigMottaker(): Boolean {
        return startRecord.mottager == "NAV"
    }

    private fun validateGyldigFiltype(): Boolean {
        return startRecord.filType == "ANV"
    }

    private fun validateGyldigFillopenummer(forrige: Int): Boolean {
        return startRecord.filLopenummer == forrige + 1
    }

    private fun validateFillopenummerIkkeBrukt(): Boolean {
        // TODO: Hent alle løpenummer brukt av SPK
        val existingLopenummer: List<Int> = listOf() //= getLopenummer("SPK")
        return existingLopenummer.contains(startRecord.filLopenummer)
    }

    private fun validateGyldigSum(): Boolean {
        return endRecord.totalBelop == totalBelop
    }

    private fun validateGyldigAntall(): Boolean {
        return endRecord.numberOfRecord == numberOfRecord
    }
}
