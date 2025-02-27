package no.nav.sokos.spk.mottak.util

import io.kotest.core.spec.style.FunSpec

class CopyBookUtilsTest : FunSpec({
    test("should return correct value") {
        val test = "123  hans marius"
        val person: Person = test.toDataClass()
        println(person)
    }
})

data class Person(
    @CopyBookField(length = 5, type = CopyBookType.INTEGER)
    val id: Int,
    @CopyBookField(length = 20, type = CopyBookType.STRING)
    val name: String,
)
