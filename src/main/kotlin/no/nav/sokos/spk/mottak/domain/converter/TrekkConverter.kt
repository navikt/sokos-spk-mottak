package no.nav.sokos.spk.mottak.domain.converter

import no.nav.sokos.spk.mottak.domain.SPK_TSS
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.oppdrag.Dokument
import no.nav.sokos.spk.mottak.domain.oppdrag.InnrapporteringTrekk
import no.nav.sokos.spk.mottak.domain.oppdrag.Periode
import no.nav.sokos.spk.mottak.util.Utils.toISOString

private const val AKSJONSKODE = "NY"

object TrekkConverter {
    fun Transaksjon.innrapporteringTrekk(): Dokument =
        Dokument(
            mmel = null,
            InnrapporteringTrekk(
                aksjonskode = AKSJONSKODE,
                kreditorIdTss = SPK_TSS,
                kreditorTrekkId = transEksId,
                debitorId = fnr,
                kodeTrekktype = trekkType!!,
                kodeTrekkAlternativ = trekkAlternativ!!,
                periode =
                    mutableListOf(
                        Periode(
                            datoFom!!.toISOString(),
                            datoTom!!.toISOString(),
                            sats = belop / 100.0,
                        ),
                    ),
            ),
        )
}
