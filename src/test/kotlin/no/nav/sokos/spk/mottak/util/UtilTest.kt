package no.nav.sokos.spk.mottak.util

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.spk.mottak.util.Utils.toLocalDate
import java.time.LocalDate

internal class UtilTest : ExpectSpec({

    context("string konverteres til LocalDate") {

        expect("returnerer LocalDate") {
            val date = "20240101".toLocalDate()
            date shouldBe LocalDate.of(2024, 1, 1)
        }

        expect("returnerer null på grunn av feil format") {
            val date = "2024-01-01".toLocalDate()
            date shouldBe null
        }

        expect("returnerer null på grunn teksten er blank") {
            val date = "".toLocalDate()
            date shouldBe null
        }
    }
})
