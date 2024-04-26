package no.nav.sokos.spk.mottak.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.sokos.spk.mottak.SPK_FILE_OK
import no.nav.sokos.spk.mottak.TestHelper.readFromResource

class FileParserTest : BehaviorSpec({

    Given("SPK innlesingsfil") {
        val innlesingsfil = SPK_FILE_OK.readFromResource()
        val innlesingRecord = innlesingsfil.lines()
        innlesingRecord.size shouldNotBe 0

        When("leser StartRecord") {
            val startRecord = FileParser.parseStartRecord(innlesingRecord.first())
            then("skal det returneres med StartRecord data") {
                startRecord.avsender shouldBe "SPK"
                startRecord.mottager shouldBe "NAV"
                startRecord.filLopenummer shouldBe 34
                startRecord.filType shouldBe "ANV"
                startRecord.produsertDato.toString() shouldBe "2024-01-31"
                startRecord.beskrivelse shouldBe "ANVISNINGSFIL"
                startRecord.rawRecord shouldBe "01SPK        NAV        000034ANV20240131ANVISNINGSFIL                      00"
            }
        }

        When("leser InnTransaksjon") {
            val inntransaksjon = FileParser.parseTransaction(innlesingRecord[1])

            Then("skal det returneres med InnTransaksjon data") {
                inntransaksjon.transId shouldBe "116684810"
                inntransaksjon.fnr shouldBe "66064900162"
                inntransaksjon.utbetalesTil shouldBe ""
                inntransaksjon.datoAnviser shouldBe "20240131"
                inntransaksjon.datoFom shouldBe "20240201"
                inntransaksjon.datoTom shouldBe "20240229"
                inntransaksjon.belopstype shouldBe "01"
                inntransaksjon.belop shouldBe "00000346900"
                inntransaksjon.art shouldBe "UFT"
                inntransaksjon.refTransId shouldBe ""
                inntransaksjon.tekstKode shouldBe ""
                inntransaksjon.saldo shouldBe "00000000410"
                inntransaksjon.prioritet shouldBe ""
                inntransaksjon.kid shouldBe ""
                inntransaksjon.trekkansvar shouldBe ""
                inntransaksjon.grad shouldBe ""
            }
        }

        When("leser EndRecord") {
            val endRecord = FileParser.parseEndRecord(innlesingRecord.last())

            Then("skal det returneres med EndRecord data") {
                endRecord.numberOfRecord shouldBe 8
                endRecord.totalBelop shouldBe 2775200
            }
        }
    }
})

