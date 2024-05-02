package no.nav.sokos.spk.mottak.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.reflect.full.memberProperties

object Util {
    fun String.toLocalDate(): LocalDate? {
        return runCatching {
            this.ifBlank { null }.let { LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyyMMdd")) }
        }.getOrNull()
    }

    fun Boolean.toChar(): Char {
        return if (this) '1' else '0'
    }

    inline fun <reified T : Any> T.asMap(): Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }
}
