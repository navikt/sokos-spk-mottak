package no.nav.sokos.spk.mottak.util

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

object Utils {
    fun String.toLocalDate(): LocalDate? {
        return runCatching {
            this.ifBlank { null }.let { LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyyMMdd")) }
        }.getOrNull()
    }

    fun LocalDate.toLocalDateString(): String {
        return this.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
    }

    fun LocalDate.toXMLGregorianCalendar(): XMLGregorianCalendar {
        return DatatypeFactory.newInstance().newXMLGregorianCalendar(this.toString())
    }

    fun Boolean.toChar(): Char {
        return if (this) '1' else '0'
    }
}
