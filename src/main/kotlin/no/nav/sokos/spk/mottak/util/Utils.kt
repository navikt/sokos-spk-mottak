package no.nav.sokos.spk.mottak.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

object Utils {
    fun String.toLocalDate(): LocalDate? =
        runCatching {
            this.ifBlank { null }.let { LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyyMMdd")) }
        }.getOrNull()

    fun LocalDate.toLocalDateString(): String = this.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    fun LocalDate.toISOString(): String = this.format(DateTimeFormatter.ISO_DATE)

    fun LocalDate.toXMLGregorianCalendar(): XMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(this.toString())

    fun Boolean.toChar(): Char = if (this) '1' else '0'
}
