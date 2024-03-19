package no.nav.sokos.spk.mottak.validator

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
        return when {
            !gyldigAvsender() -> FileStatusValidation.UGYLDIG_ANVISER
            !gyldigMottaker() -> FileStatusValidation.UGYLDIG_MOTTAKER
            !gyldigFiltype() -> FileStatusValidation.UGYLDIG_FILTYPE
            !fillopenummerIkkeBrukt(maxLopenummer) -> FileStatusValidation.FILLOPENUMMER_I_BRUK
            !gyldigFillopenummer(maxLopenummer) -> FileStatusValidation.UGYLDIG_FILLOPENUMMER
            !gyldigSum() -> FileStatusValidation.UGYLDIG_SUMBELOP
            !gyldigAntall() -> FileStatusValidation.UGYLDIG_ANTRECORDS
            else -> FileStatusValidation.OK
        }
    }

    private fun gyldigAvsender(): Boolean {
        return startRecord.avsender == SPK
    }

    private fun gyldigMottaker(): Boolean {
        return startRecord.mottager == NAV
    }

    private fun gyldigFiltype(): Boolean {
        return startRecord.filType == FILETYPE_ANVISER
    }

    private fun gyldigFillopenummer(maxLopenummer: Int): Boolean {
        return startRecord.filLopenummer == (maxLopenummer + 1)
    }

    private fun fillopenummerIkkeBrukt(maxLopenummer: Int): Boolean {
        return maxLopenummer < startRecord.filLopenummer
    }

    private fun gyldigSum(): Boolean {
        return endRecord.totalBelop == totalBelop
    }

    private fun gyldigAntall(): Boolean {
        return endRecord.numberOfRecord == numberOfRecord
    }
}
