package no.nav.sokos.spk.mottak.validator

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class QueryParameterValidatorTest : FunSpec({

    test("validateDateQueryParameter skal returnere dato hvis den er gyldig") {
        val date = "2021-01-01"
        val result = date.validateDateQueryParameter()
        result shouldBe date
    }

    test("validateDateQueryParameter skal kaste IllegalArgumentException hvis dato ikke er på formatet yyyy-MM-dd") {
        val date = "2021-01-feilformat"
        val exception =
            shouldThrow<IllegalArgumentException> {
                date.validateDateQueryParameter()
            }
        exception.message shouldBe INVALID_DATE_QUERY_PARAMETER_MESSAGE
    }
})
