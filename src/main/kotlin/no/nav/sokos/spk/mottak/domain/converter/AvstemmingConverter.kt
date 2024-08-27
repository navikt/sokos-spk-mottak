package no.nav.sokos.spk.mottak.domain.converter

import no.nav.sokos.spk.mottak.domain.MOT
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AksjonType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Aksjonsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.AvstemmingType
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.Avstemmingsdata
import no.nav.virksomhet.tjenester.avstemming.meldinger.v1.KildeType
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID

private const val MOTTAKENDE_KOMPONENT_KODE = "OS"

object AvstemmingConverter {
    fun startMelding(
        fom: LocalDateTime,
        tom: LocalDateTime,
        fagomrade: String,
    ): Avstemmingsdata =
        Avstemmingsdata().apply {
            aksjon =
                Aksjonsdata().apply {
                    aksjonType = AksjonType.START
                    kildeType = KildeType.AVLEV
                    avstemmingType = AvstemmingType.GRSN
                    avleverendeKomponentKode = "??"
                    mottakendeKomponentKode = MOTTAKENDE_KOMPONENT_KODE
                    underkomponentKode = fagomrade
                    nokkelFom = fom.format(DateTimeFormatter.ISO_DATE_TIME)
                    nokkelFom = tom.format(DateTimeFormatter.ISO_DATE_TIME)
                    avleverendeAvstemmingId = UUID.randomUUID().toString()
                    brukerId = MOT
                }
        }

    fun Avstemmingsdata.sluttMelding() =
        Avstemmingsdata().apply {
            aksjon = this.aksjon.apply { aksjonType = AksjonType.AVSL }
        }

    fun Avstemmingsdata.dataMelding(): Avstemmingsdata =
        Avstemmingsdata().apply {
            aksjon = this.aksjon.apply { AksjonType.DATA }
        }

    fun Avstemmingsdata.avvikMelding(): Avstemmingsdata =
        Avstemmingsdata().apply {
            aksjon = this.aksjon.apply { AksjonType.DATA }
        }
}
