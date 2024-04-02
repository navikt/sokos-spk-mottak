package no.nav.sokos.spk.mottak.util

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import no.nav.sokos.spk.mottak.util.StringUtil.toLocalDate

class StringUtilTest : ExpectSpec({

    context("string konverteres til LocalDate") {

        expect("returnerer LocalDate") {
            val dato = "20240101".toLocalDate()
            dato shouldBe LocalDate.of(2024, 1, 1)
        }

        expect("returnerer null på grunn av feil format") {
            val dato = "2024-01-01".toLocalDate()
            dato shouldBe null
        }

        expect("returnerer null på grunn teksten er blank") {
            val dato = "".toLocalDate()
            dato shouldBe null
        }
    }
})