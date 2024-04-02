package no.nav.sokos.spk.mottak.validator

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.spk.mottak.TestData

class FileValidationTest : ExpectSpec({

    context("validering av SPK innlesningsfil") {

        expect("FileStatus.OK når filen er ok") {
            val recordData = TestData.recordDataMock()
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.OK
        }

        expect("FileStatus.UGYLDIG_ANVISER når avsender er ugyldig") {
            val recordData = TestData.recordDataMock().copy(
                startRecord = TestData.startRecordMock().copy(avsender = "TEST")
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_ANVISER
        }

        expect("FileStatus.UGYLDIG_MOTTAKER når mottager er ugyldig") {
            val recordData = TestData.recordDataMock().copy(
                startRecord = TestData.startRecordMock().copy(mottager = "TEST")
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_MOTTAKER
        }

        expect("FileStatus.UGYLDIG_FILTYPE når filtype er ugyldig") {
            val recordData = TestData.recordDataMock().copy(
                startRecord = TestData.startRecordMock().copy(filType = "TEST")
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_FILTYPE
        }

        expect("FileStatus.FILLOPENUMMER_I_BRUK når fillopenummer er i bruk") {
            val recordData = TestData.recordDataMock().copy(
                maxLopenummer = 123
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.FILLOPENUMMER_I_BRUK
        }

        expect("FileStatus.UGYLDIG_FILLOPENUMMER når fillopenummer er ugyldig") {
            val recordData = TestData.recordDataMock().copy(
                maxLopenummer = 121
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_FILLOPENUMMER
        }

        expect("FileStatus.UGYLDIG_SUMBELOP når sumbeløp er ugyldig") {
            val recordData = TestData.recordDataMock().copy(
                totalBelop = 0
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_SUMBELOP
        }

        expect("FileStatus.UGYLDIG_ANTRECORDS når antall records er ugyldig") {
            val recordData = TestData.recordDataMock().copy(
                endRecord = TestData.endRecordMock().copy(numberOfRecord = 9)
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_ANTRECORDS
        }
    }
})