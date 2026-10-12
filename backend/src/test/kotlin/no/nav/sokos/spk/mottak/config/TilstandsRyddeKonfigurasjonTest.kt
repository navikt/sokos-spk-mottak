package no.nav.sokos.spk.mottak.config

import java.time.Duration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

class TilstandsRyddeKonfigurasjonTest : FunSpec({
    test("dokumenterte heltallsnøkler overstyrer alle standardverdier") {
        val verdier = mapOf(
            "OPPBEVARING_MAANEDER" to "12", "INTERVALL_BREDDE" to "10", "BATCH_STORRELSE" to "5",
            "MAKS_REFERANSER_PER_BATCH" to "20", "PAUSE_MIN_SEKUNDER" to "2",
            "PAUSE_MAKS_SEKUNDER" to "40", "TERSKEL_ENHET_SEKUNDER" to "3",
            "MAKS_KJOERETID_SEKUNDER" to "60", "MAKS_FORSOEK" to "4", "SPORRING_TIMEOUT_SEKUNDER" to "7",
        ).mapKeys { "TILSTANDS_RYDDING_${it.key}" }
        PropertiesConfig.tilstandsRyddeKonfigurasjon { verdier[it].orEmpty() } shouldBe
            TilstandsRyddeKonfigurasjon(
                12, 10, 5, 20, Duration.ofSeconds(2), Duration.ofSeconds(40), Duration.ofSeconds(3),
                Duration.ofSeconds(60), 4, Duration.ofSeconds(7),
            )
    }
    test("manglende verdier gir standardkonfigurasjon") {
        PropertiesConfig.tilstandsRyddeKonfigurasjon { "" } shouldBe TilstandsRyddeKonfigurasjon()
    }
    test("ugyldige og brøkdelte tall og ISO-varigheter avvises") {
        for (nokkel in listOf("PAUSE_MIN_SEKUNDER", "MAKS_FORSOEK", "SPORRING_TIMEOUT_SEKUNDER")) {
            for (verdi in listOf("1.5", "PT1S", "ikke-tall", "0", "-1", "9223372036854775808")) {
                shouldThrow<IllegalArgumentException> {
                    PropertiesConfig.tilstandsRyddeKonfigurasjon {
                        if (it == "TILSTANDS_RYDDING_$nokkel") verdi else ""
                    }
                }
            }
        }
    }
})
