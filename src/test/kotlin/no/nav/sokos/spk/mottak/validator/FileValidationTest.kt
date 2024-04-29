package no.nav.sokos.spk.mottak.validator

import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.spk.mottak.TestData
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.exception.FilValidationException

class FileValidationTest : ExpectSpec({

    context("validering av SPK innlesningsfil") {

        expect("Ingen feil blir kastet når filen er OK validert") {
            shouldNotThrowAny {
                FileValidation.validateStartAndSluttRecord(TestData.recordDataMock())
            }
        }

        expect("FileStatus.UGYLDIG_ANVISER når avsender er ugyldig") {
            val exception = shouldThrow<FilValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    startRecord = TestData.startRecordMock().copy(avsender = "TEST")
                )
                FileValidation.validateStartAndSluttRecord(recordData)
            }

            exception.statusCode shouldBe FilStatus.UGYLDIG_ANVISER.code
            exception.message shouldBe FilStatus.UGYLDIG_ANVISER.message
        }

        expect("FileStatus.UGYLDIG_MOTTAKER når mottager er ugyldig") {
            val exception = shouldThrow<FilValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    startRecord = TestData.startRecordMock().copy(mottager = "TEST")
                )
                FileValidation.validateStartAndSluttRecord(recordData)
            }
            exception.statusCode shouldBe FilStatus.UGYLDIG_MOTTAKER.code
            exception.message shouldBe FilStatus.UGYLDIG_MOTTAKER.message
        }

        expect("FileStatus.UGYLDIG_FILTYPE når filtype er ugyldig") {
            val exception = shouldThrow<FilValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    startRecord = TestData.startRecordMock().copy(filType = "TEST")
                )
                FileValidation.validateStartAndSluttRecord(recordData)
            }
            exception.statusCode shouldBe FilStatus.UGYLDIG_FILTYPE.code
            exception.message shouldBe FilStatus.UGYLDIG_FILTYPE.message
        }

        expect("FileStatus.FILLOPENUMMER_I_BRUK når fillopenummer er i bruk") {
            val exception = shouldThrow<FilValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    maxLopenummer = 123
                )
                FileValidation.validateStartAndSluttRecord(recordData)
            }
            exception.statusCode shouldBe FilStatus.FILLOPENUMMER_I_BRUK.code
            exception.message shouldBe "Filløpenummer 123 allerede i bruk"
        }

        expect("FileStatus.FORVENTER_FILLOPENUMMER når fillopenummer er ugyldig") {
            val exception = shouldThrow<FilValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    maxLopenummer = 121
                )
                FileValidation.validateStartAndSluttRecord(recordData)
            }
            exception.statusCode shouldBe FilStatus.FORVENTET_FILLOPENUMMER.code
            exception.message shouldBe "Forventet lopenummer 122"
        }

        expect("FileStatus.UGYLDIG_SUMBELOP når sumbeløp i sluttrecord ikke stemmer med oppsummering av enkeltbeløpene") {
            val exception = shouldThrow<FilValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    totalBelop = 100
                )
                FileValidation.validateStartAndSluttRecord(recordData)
            }
            exception.statusCode shouldBe FilStatus.UGYLDIG_SUMBELOP.code
            exception.message shouldBe "Total beløp 2775200 stemmer ikke med summeringen av enkelt beløpene 100"
        }

        expect("FileStatus.UGYLDIG_SUMBELOP når sumbeløp i sluttrecord er ugyldig") {
            val exception = shouldThrow<FilValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    sluttRecord = TestData.sluttRecordMock().copy(totalBelop = 0)
                )
                FileValidation.validateStartAndSluttRecord(recordData)
            }
            exception.statusCode shouldBe FilStatus.UGYLDIG_SUMBELOP.code
            exception.message shouldBe "Total beløp 0 stemmer ikke med summeringen av enkelt beløpene 2775200"
        }

        expect("FileStatus.UGYLDIG_ANTRECORDS når antall records er feil i sluttrecord") {
            val exception = shouldThrow<FilValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    sluttRecord = TestData.sluttRecordMock().copy(antallRecord = 9)
                )
                FileValidation.validateStartAndSluttRecord(recordData)
            }
            exception.statusCode shouldBe FilStatus.UGYLDIG_ANTRECORDS.code
            exception.message shouldBe "Oppsumert antall records 9 stemmer ikke med det faktiske antallet 8"
        }

        expect("FileStatus.UGYLDIG_ANTRECORDS når antall records er ugyldig i sluttercord") {
            val exception = shouldThrow<FilValidationException> {
                val recordData = TestData.recordDataMock().copy(
                    sluttRecord = TestData.sluttRecordMock().copy(antallRecord = 0)
                )
                FileValidation.validateStartAndSluttRecord(recordData)
            }
            exception.statusCode shouldBe FilStatus.UGYLDIG_ANTRECORDS.code
            exception.message shouldBe "Oppsumert antall records 0 stemmer ikke med det faktiske antallet 8"
        }
    }
})