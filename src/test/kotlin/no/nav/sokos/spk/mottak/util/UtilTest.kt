package no.nav.sokos.spk.mottak.util

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import javax.xml.datatype.DatatypeFactory
import no.nav.sokos.spk.mottak.util.Utils.booleanToString
import no.nav.sokos.spk.mottak.util.Utils.toAvstemmingPeriode
import no.nav.sokos.spk.mottak.util.Utils.toISOString
import no.nav.sokos.spk.mottak.util.Utils.toLocalDate
import no.nav.sokos.spk.mottak.util.Utils.toLocalDateString
import no.nav.sokos.spk.mottak.util.Utils.toXMLGregorianCalendar

internal class UtilTest :
    ExpectSpec({
        context("string konverteres til LocalDate") {
            expect("returnerer LocalDate") {
                "20240101".toLocalDate() shouldBe LocalDate.of(2024, 1, 1)
            }

            expect("returnerer null på grunn av feil format") {
                "2024-01-01".toLocalDate() shouldBe null
            }

            expect("returnerer null på grunn teksten er blank") {
                "".toLocalDate() shouldBe null
            }
        }

        context("LocalDate konverteres til string") {
            val date = LocalDate.of(2024, 1, 1)
            expect("returnerer string") {
                date.toLocalDateString() shouldBe "20240101"
            }

            expect("returnerer til ISO format") {
                date.toISOString() shouldBe "2024-01-01"
            }
        }

        context("LocalDate konverteres til XMLGregorianCalendar") {
            expect("returnerer XMLGregorianCalendar") {
                LocalDate.of(2024, 1, 1).toXMLGregorianCalendar() shouldBe
                    DatatypeFactory.newInstance().newXMLGregorianCalendar("2024-01-01")
            }
        }

        context("Boolean konverteres til String") {
            expect("returnerer String") {
                true.booleanToString() shouldBe "1"
                false.booleanToString() shouldBe "0"
            }
        }

        context("LocalDateTime konverteres til avstemmingPeriode") {
            expect("returnerer avstemmingPeriode") {
                LocalDateTime.of(2024, 1, 1, 12, 0).toAvstemmingPeriode() shouldBe "2024010112"
            }
        }
    })
