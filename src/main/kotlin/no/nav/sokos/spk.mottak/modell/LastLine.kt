package no.nav.sokos.spk.mottak.modell

import java.math.BigDecimal

class LastLine(
    val antallLinjer: Int,
    val sumAlleLinjer: BigDecimal
) {
    override fun toString(): String {
        return "LastLine(antallLinjer=$antallLinjer, sumAlleLinjer=$sumAlleLinjer)"
    }
}