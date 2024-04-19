package no.nav.sokos.spk.mottak.util

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.sokos.spk.mottak.SPK_FILE_OK
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.validator.FileStatus

class FileParserTest : BehaviorSpec({

    given("SPK innlesingsfil") {
        val innlesingsfil = SPK_FILE_OK.readFromResource()
        val innlesingRecord = innlesingsfil.lines()
        innlesingRecord.size shouldNotBe 0

        `when`("leser StartRecord") {
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

        `when`("leser InnTransaksjon") {
            val inntransaksjon = FileParser.parseTransaction(innlesingRecord[1])
            then("skal det returneres med InnTransaksjon data") {
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

        `when`("leser EndRecord") {
            val endRecord = FileParser.parseEndRecord(innlesingRecord.last())
            then("skal det returneres med EndRecord data") {
                endRecord.numberOfRecord shouldBe 8
                endRecord.totalBelop shouldBe 2775200
            }
        }
    }

    given("SPK innlesingsfil med ugyldig innhold i StartRecord") {
        `when`("StartRecord innholder ugyldig record type") {
            val innlesingRecord = "02SPK        NAV        000034ANV20240131ANVISNINGSFIL                      00"
            val startRecord = FileParser.parseStartRecord(innlesingRecord)
            then("skal det returneres UGYLDIG_RECTYPE") {
                startRecord.avsender shouldBe "SPK"
                startRecord.mottager shouldBe "NAV"
                startRecord.filLopenummer shouldBe 34
                startRecord.filType shouldBe "ANV"
                startRecord.produsertDato.toString() shouldBe "2024-01-31"
                startRecord.beskrivelse shouldBe "ANVISNINGSFIL"
                startRecord.fileStatus shouldBe FileStatus.UGYLDIG_RECTYPE
                startRecord.rawRecord shouldBe "02SPK        NAV        000034ANV20240131ANVISNINGSFIL                      00"
            }
        }

        `when`("StartRecord innholder ugyldig produsert dato") {
            val innlesingRecord = "01SPK        NAV        000034ANV20241331ANVISNINGSFIL                      00"
            val startRecord = FileParser.parseStartRecord(innlesingRecord)
            then("skal det returneres UGYLDIG_PRODDATO") {
                startRecord.avsender shouldBe "SPK"
                startRecord.mottager shouldBe "NAV"
                startRecord.filLopenummer shouldBe 34
                startRecord.filType shouldBe "ANV"
                startRecord.produsertDato shouldBe null
                startRecord.beskrivelse shouldBe "ANVISNINGSFIL"
                startRecord.fileStatus shouldBe FileStatus.UGYLDIG_PRODDATO
                startRecord.rawRecord shouldBe "01SPK        NAV        000034ANV20241331ANVISNINGSFIL                      00"
            }
        }

        `when`("StartRecord innholder ugyldig fil lopenummer") {
            val innlesingRecord = "01SPK        NAV        00003rANV20240131ANVISNINGSFIL                      00"
            val startRecord = FileParser.parseStartRecord(innlesingRecord)
            then("skal det returneres UGYLDIG_FILLOPENUMMER") {
                startRecord.avsender shouldBe "SPK"
                startRecord.mottager shouldBe "NAV"
                startRecord.filLopenummer shouldBe 0
                startRecord.filType shouldBe "ANV"
                startRecord.produsertDato.toString() shouldBe "2024-01-31"
                startRecord.beskrivelse shouldBe "ANVISNINGSFIL"
                startRecord.fileStatus shouldBe FileStatus.UGYLDIG_FILLOPENUMMER
                startRecord.rawRecord shouldBe "01SPK        NAV        00003rANV20240131ANVISNINGSFIL                      00"
            }
        }
    }

    given("Ugyldig innhold i InnTransaksjon i SPK innlesingsfil") {
        val innlesingRecord =
            "09116684810   66064900162           2024013120240201202402290100000346900UFT                 00000000410"
        val transaksjonRecord = FileParser.parseTransaction(innlesingRecord)
        then("skal det returneres UGYLDIG_RECTYPE") {
            transaksjonRecord.transId shouldBe "116684810"
            transaksjonRecord.fnr shouldBe "66064900162"
            transaksjonRecord.art shouldBe "UFT"
            transaksjonRecord.belop.toInt() shouldBe 346900
            transaksjonRecord.belopstype shouldBe "01"
            transaksjonRecord.datoAnviser.toString() shouldBe "20240131"
            transaksjonRecord.datoFom.toString() shouldBe "20240201"
            transaksjonRecord.datoTom.toString() shouldBe "20240229"
            transaksjonRecord.fileStatus shouldBe FileStatus.UGYLDIG_RECTYPE
        }
    }

    given("Ugyldig innhold i EndRecord i SPK innlesingsfil") {
        val innlesingRecord = "000000000080000002775200"
        val endRecord = FileParser.parseEndRecord(innlesingRecord)
        then("skal det returneres UGYLDIG_RECTYPE") {
            endRecord.numberOfRecord shouldBe 8
            endRecord.totalBelop shouldBe 2775200
            endRecord.fileStatus shouldBe FileStatus.UGYLDIG_RECTYPE
        }
    }
})

