package no.nav.sokos.spk.mottak

import mu.KotlinLogging
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}

object TestHelper {
    fun readFromResource(filename: String): String {
        val inputStream = this::class.java.getResourceAsStream(filename)!!
        return BufferedReader(InputStreamReader(inputStream)).lines()
            .parallel().collect(Collectors.joining("\n"))
    }
}
