package no.nav.sokos.spk.mottak.validator

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.spk.mottak.TestData

class FileValidationTest : ExpectSpec({
    context("Skal fileStatus") {

        expect("returnere med FileStatus.OK") {
            val recordData = TestData.recordDataMock()
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.OK
        }

        expect("returnere med FileStatus.UGYLDIG_ANVISER") {
            val recordData = TestData.recordDataMock().copy(
                startRecord = TestData.startRecordMock().copy(avsender = "TEST")
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_ANVISER
        }

        expect("returnere med FileStatus.UGYLDIG_MOTTAKER") {
            val recordData = TestData.recordDataMock().copy(
                startRecord = TestData.startRecordMock().copy(mottager = "TEST")
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_MOTTAKER
        }

        expect("returnere med FileStatus.UGYLDIG_FILTYPE") {
            val recordData = TestData.recordDataMock().copy(
                startRecord = TestData.startRecordMock().copy(filType = "TEST")
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_FILTYPE
        }

        expect("returnere med FileStatus.FILLOPENUMMER_I_BRUK") {
            val recordData = TestData.recordDataMock().copy(
                maxLopenummer = 123
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.FILLOPENUMMER_I_BRUK
        }

        expect("returnere med FileStatus.UGYLDIG_FILLOPENUMMER") {
            val recordData = TestData.recordDataMock().copy(
                maxLopenummer = 121
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_FILLOPENUMMER
        }

        expect("returnere med FileStatus.UGYLDIG_SUMBELOP") {
            val recordData = TestData.recordDataMock().copy(
                totalBelop = 0
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_SUMBELOP
        }

        expect("returnere med FileStatus.UGYLDIG_ANTRECORDS") {
            val recordData = TestData.recordDataMock().copy(
                endRecord = TestData.endRecordMock().copy(numberOfRecord = 9)
            )
            val fileStatus = FileValidation.validateStartAndEndRecord(recordData)
            fileStatus shouldBe FileStatus.UGYLDIG_ANTRECORDS
        }
    }
})