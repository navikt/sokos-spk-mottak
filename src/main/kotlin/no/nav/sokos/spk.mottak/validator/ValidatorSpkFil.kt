package no.nav.sokos.spk.mottak.validator

import mu.KotlinLogging
import no.nav.sokos.spk.mottak.modell.FirstLine
import no.nav.sokos.spk.mottak.modell.LastLine
import java.math.BigDecimal

class ValidatorSpkFil(val first: FirstLine, val last: LastLine, val antallLinjer: Int, val totalBelop: BigDecimal) {

    private val log = KotlinLogging.logger {}
    fun validerLines(): SpkFilformatFeil {
        try {
            require(validerGyldigAvsender()) { SpkFilformatFeil.AVSENDER }
            require(validerGyldigMottaker()) { SpkFilformatFeil.MOTTAGER }
            require(validerGyldigFiltype()) { SpkFilformatFeil.FILTYPE }
            require(validerGyldigFilløpenummer(hentForrigeLopenummer())) { SpkFilformatFeil.FILLOPENUMMER }
            require(validerGyldigSum()) { SpkFilformatFeil.SUM }
            require(validerGyldigAntall()) { SpkFilformatFeil.ANTALL }
        } catch (e: Exception) {
            log.error("Valideringsfeil: ${e.message}")
            SpkFilformatFeil.PARSING
        }
        return SpkFilformatFeil.INGEN_FEIL
    }

    // TODO hente forrige løpenummer i T_LOPENR
    fun hentForrigeLopenummer() = 1;
    fun validerGyldigAvsender(): Boolean {
        return first.avsender.equals("SPK")
    }

    fun validerGyldigMottaker(): Boolean {
        return first.mottager.equals("NAV")
    }

    fun validerGyldigFiltype(): Boolean {
        return first.filType.equals("ANV")
    }

    fun validerGyldigFilløpenummer(forrige: Int): Boolean {
        return first.filLopenummer.equals(forrige + 1)
    }

    fun validerGyldigSum(): Boolean {
        return last.sumAlleLinjer.equals(totalBelop)
    }

    fun validerGyldigAntall(): Boolean {
        return last.antallLinjer.equals(antallLinjer)
    }
}
