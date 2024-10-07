package no.nav.sokos.spk.mottak.domain.converter

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import no.nav.sokos.spk.mottak.domain.SPK_TSS
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_RETUR_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_RETUR_OK
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.oppdrag.Dokument
import no.nav.sokos.spk.mottak.domain.oppdrag.DokumentWrapper
import no.nav.sokos.spk.mottak.domain.oppdrag.InnrapporteringTrekk
import no.nav.sokos.spk.mottak.domain.oppdrag.Mmel
import no.nav.sokos.spk.mottak.domain.oppdrag.Periode
import no.nav.sokos.spk.mottak.domain.oppdrag.Perioder
import no.nav.sokos.spk.mottak.util.Utils.toISOString

private const val AKSJONSKODE = "NY"

object TrekkConverter {
    private val json = Json { ignoreUnknownKeys = true }

    fun Transaksjon.innrapporteringTrekk(): String = json.encodeToString(
        DokumentWrapper(
            dokument = Dokument(
                transaksjonsId = transaksjonId!!.toString(),
                InnrapporteringTrekk(
                    aksjonskode = AKSJONSKODE,
                    kreditorIdTss = SPK_TSS,
                    kreditorTrekkId = transEksId,
                    debitorId = fnr,
                    kodeTrekktype = trekkType!!,
                    kodeTrekkAlternativ = trekkAlternativ!!,
                    perioder = Perioder(
                        mutableListOf(
                            Periode(
                                datoFom!!.toISOString(),
                                datoTom!!.toISOString(),
                                sats = belop / 100.0,
                            ),
                        ),
                    ),
                ),
            ),
        ),
    )

    fun Mmel.trekkStatus(): String = when {
        alvorlighetsgrad?.toInt()!! < 5 -> TRANS_TILSTAND_TREKK_RETUR_OK
        else -> TRANS_TILSTAND_TREKK_RETUR_FEIL
    }
}
