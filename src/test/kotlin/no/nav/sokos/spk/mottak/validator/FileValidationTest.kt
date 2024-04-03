package no.nav.sokos.spk.mottak.validator

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.spk.mottak.TestData
import no.nav.sokos.spk.mottak.exception.ValidationException

class FileValidationTest : ExpectSpec({

    context("validering av SPK innlesningsfil") {

        expect("Ingen feil blir kastet når filen er OK validert") {
            shouldNotThrowAny {
                FileValidation.validateStartAndEndRecord(TestData.recordDataMock())
            }
        }

        expect("FileStatus.UGYLDIG_ANVISER når avsender er ugyldig") {
            val exception = shouldThrow<ValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    startRecord = TestData.startRecordMock().copy(avsender = "TEST")
                )
                FileValidation.validateStartAndEndRecord(recordData)
            }

            exception.statusCode shouldBe FileStatus.UGYLDIG_ANVISER.code
            exception.message shouldBe FileStatus.UGYLDIG_ANVISER.message
        }

        expect("FileStatus.UGYLDIG_MOTTAKER når mottager er ugyldig") {
            val exception = shouldThrow<ValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    startRecord = TestData.startRecordMock().copy(mottager = "TEST")
                )
                FileValidation.validateStartAndEndRecord(recordData)
            }
            exception.statusCode shouldBe FileStatus.UGYLDIG_MOTTAKER.code
            exception.message shouldBe FileStatus.UGYLDIG_MOTTAKER.message
        }

        expect("FileStatus.UGYLDIG_FILTYPE når filtype er ugyldig") {
            val exception = shouldThrow<ValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    startRecord = TestData.startRecordMock().copy(filType = "TEST")
                )
                FileValidation.validateStartAndEndRecord(recordData)
            }
            exception.statusCode shouldBe FileStatus.UGYLDIG_FILTYPE.code
            exception.message shouldBe FileStatus.UGYLDIG_FILTYPE.message
        }

        expect("FileStatus.FILLOPENUMMER_I_BRUK når fillopenummer er i bruk") {
            val exception = shouldThrow<ValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    maxLopenummer = 123
                )
                FileValidation.validateStartAndEndRecord(recordData)
            }
            exception.statusCode shouldBe FileStatus.FILLOPENUMMER_I_BRUK.code
            exception.message shouldBe "Filløpenummer 123 allerede i bruk"
        }

        expect("FileStatus.FORVENTER_FILLOPENUMMER når fillopenummer er ugyldig") {
            val exception = shouldThrow<ValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    maxLopenummer = 121
                )
                FileValidation.validateStartAndEndRecord(recordData)
            }
            exception.statusCode shouldBe FileStatus.FORVENTER_FILLOPENUMMER.code
            exception.message shouldBe "Forventet lopenummer 122"
        }

        expect("FileStatus.UGYLDIG_SUMBELOP når sumbeløp er ugyldig") {
            val exception = shouldThrow<ValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    totalBelop = 0
                )
                FileValidation.validateStartAndEndRecord(recordData)
            }
            exception.statusCode shouldBe FileStatus.UGYLDIG_SUMBELOP.code
            exception.message shouldBe "Total beløp 0 stemmer ikke med summeringen av enkelt beløpene"
        }

        expect("FileStatus.UGYLDIG_ANTRECORDS når antall records er ugyldig") {
            val exception = shouldThrow<ValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    endRecord = TestData.endRecordMock().copy(numberOfRecord = 9)
                )
                FileValidation.validateStartAndEndRecord(recordData)
            }
            exception.statusCode shouldBe FileStatus.UGYLDIG_ANTRECORDS.code
            exception.message shouldBe "Oppsumert antall records 8 stemmer ikke med det faktiske antallet"
        }
    }
})