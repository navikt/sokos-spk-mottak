package no.nav.sokos.spk.mottak.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.sokos.spk.mottak.SPK_OK
import no.nav.sokos.spk.mottak.TestHelper.readFromResource

internal class FileParserTest : BehaviorSpec({

    Given("SPK innlesingsfil") {
        val spkFil = readFromResource("/spk/$SPK_OK")
        val recordData = spkFil.lines()
        recordData.size shouldNotBe 0

        When("leser StartRecord") {
            val startRecord = FileParser.parseStartRecord(recordData.first())
            then("skal det returneres med StartRecord data") {
                startRecord.avsender shouldBe "SPK"
                startRecord.mottager shouldBe "NAV"
                startRecord.filLopenummer shouldBe 34
                startRecord.filType shouldBe "ANV"
                startRecord.produsertDato.toString() shouldBe "2024-01-31"
                startRecord.beskrivelse shouldBe "ANVISNINGSFIL"
                startRecord.kildeData shouldBe "01SPK        NAV        000034ANV20240131ANVISNINGSFIL                      00"
            }
        }

        When("leser InnTransaksjon") {
            val transaksjonRecord = FileParser.parseTransaksjonRecord(recordData[1])

            Then("skal det returneres med InnTransaksjon data") {
                transaksjonRecord.transId shouldBe "116684810"
                transaksjonRecord.fnr shouldBe "66064900162"
                transaksjonRecord.utbetalesTil shouldBe ""
                transaksjonRecord.datoAnviser shouldBe "20240131"
                transaksjonRecord.datoFom shouldBe "20240201"
                transaksjonRecord.datoTom shouldBe "20240229"
                transaksjonRecord.belopstype shouldBe "01"
                transaksjonRecord.belop shouldBe "00000346900"
                transaksjonRecord.art shouldBe "UFT"
                transaksjonRecord.refTransId shouldBe ""
                transaksjonRecord.tekstkode shouldBe ""
                transaksjonRecord.saldo shouldBe "00000000410"
                transaksjonRecord.prioritet shouldBe ""
                transaksjonRecord.kid shouldBe ""
                transaksjonRecord.trekkansvar shouldBe ""
                transaksjonRecord.grad shouldBe ""
            }
        }

        When("leser EndRecord") {
            val sluttRecord = FileParser.parseSluttRecord(recordData.last())

            Then("skal det returneres med EndRecord data") {
                sluttRecord.antallRecord shouldBe 8
                sluttRecord.totalBelop shouldBe 2775200
            }
        }
    }
})
