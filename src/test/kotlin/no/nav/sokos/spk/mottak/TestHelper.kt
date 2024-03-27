package no.nav.sokos.spk.mottak

object TestHelper {
    fun String.readFromResource() = {}::class.java.classLoader.getResource(this)!!.readText()
}