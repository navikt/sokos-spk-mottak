package no.nav.sokos.spk.mottak.util

import java.text.ParseException
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.xml.datatype.DatatypeFactory
import javax.xml.datatype.XMLGregorianCalendar

object Utils {
    fun String.toLocalDate(): LocalDate? =

        runCatching {
            this.ifBlank { null }.let { LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyyMMdd")) }
        }.getOrNull()

    fun String.toIsoDate(): LocalDate? =
        runCatching {
            this.ifBlank { null }.let { LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyy-MM-dd")) }
        }.getOrNull()

    fun LocalDate.toLocalDateString(): String = this.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    fun LocalDate.toISOString(): String = this.format(DateTimeFormatter.ISO_DATE)

    fun LocalDate.toXMLGregorianCalendar(): XMLGregorianCalendar = DatatypeFactory.newInstance().newXMLGregorianCalendar(this.toString())

    fun Boolean.booleanToString(): String = if (this) "1" else "0"

    fun LocalDateTime.toAvstemmingPeriode(): String = this.format(DateTimeFormatter.ofPattern("yyyyMMddHH"))

    fun String.toLocalDateStringOrEmpty(): String {
        return this.trim().toIntOrNull()?.let { if (it == 0) "" else this } ?: throw ParseException("Feil ved konvertering av $this to datoformat 'yyyyMMdd'", 0)
    }

    fun String.toLocalDateNotBlank(): LocalDate {
        if (this.isBlank()) {
            throw ParseException("Ikke tillatt med blank dato-streng", 0)
        }
        return try {
            LocalDate.parse(this, DateTimeFormatter.ofPattern("yyyyMMdd"))
        } catch (e: DateTimeParseException) {
            throw ParseException("Feil ved konvertering av $this (format yyyyMMdd) til dato", 0)
        }
    }
}
