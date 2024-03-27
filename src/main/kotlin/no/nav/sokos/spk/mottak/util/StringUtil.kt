package no.nav.sokos.spk.mottak.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter

object StringUtil {
    fun String.toLocalDate(): LocalDate? =
        runCatching {
            this.ifBlank { null }.let { LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyyMMdd")) }
        }.getOrNull()
}
