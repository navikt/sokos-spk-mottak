package no.nav.sokos.spk.mottak.service

import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.modell.EndRecord
import no.nav.sokos.spk.mottak.modell.StartRecord
import no.nav.sokos.spk.mottak.modell.Transaction
import no.nav.sokos.spk.mottak.validator.ValidationFileStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun parseStartRecord(record: String): StartRecord {
    val parser = SpkFilParser(record)
    if (parser.parseString(2) != "01") {
        throw ValidationException(
            ValidationFileStatus.UGYLDIG_RECTYPE.code,
            ValidationFileStatus.UGYLDIG_RECTYPE.message
        )
    }

    try {
        return StartRecord(
            avsender = parser.parseString(11),
            mottager = parser.parseString(11),
            filLopenummer = parser.parseInt(6),
            filType = parser.parseString(3),
            produsertDato = parser.parseDate(8),
            beskrivelse = parser.parseString(35)
        )
    } catch (e: DateTimeParseException) {
        throw ValidationException(
            ValidationFileStatus.UGYLDIG_PRODDATO.code,
            ValidationFileStatus.UGYLDIG_PRODDATO.message
        )
    } catch (e: NumberFormatException) {
        throw ValidationException(
            ValidationFileStatus.UGYLDIG_FILLOPENUMMER.code,
            ValidationFileStatus.UGYLDIG_FILLOPENUMMER.message
        )
    }
}

fun parseTransaction(record: String): Transaction {
    val parser = SpkFilParser(record)
    parser.parseString(2)
    return Transaction(
        transId = parser.parseString(12),
        gjelderId = parser.parseString(11),
        utbetalesTil = parser.parseString(11),
        datoAnviserStr = parser.parseString(8),
        periodeFomStr = parser.parseString(8),
        periodeTomStr = parser.parseString(8),
        belopsType = parser.parseString(2),
        belopStr = parser.parseString(9),
        art = parser.parseString(4),
        refTransId = parser.parseString(12),
        tekstKode = parser.parseString(4),
        saldoStr = parser.parseString(9),
        prioritetStr = parser.parseString(8),
        kid = parser.parseString(26),
        trekkansvar = parser.parseString(4),
        gradStr = parser.parseString(4)
    ).apply {
        this.datoAnviser = parser.parseDate(this.datoAnviserStr)
        this.periodeFom = parser.parseDate(this.periodeFomStr)
        this.periodeTom = parser.parseDate(this.periodeTomStr)
        this.belop = parser.parseInt(this.belopStr)
        this.saldo = parser.parseInt(this.saldoStr)
        this.prioritet = parser.parseDate(this.prioritetStr)
        this.grad = parser.parseInt(this.gradStr)
    }
}

fun parseEndRecord(record: String): EndRecord {
    val parser = SpkFilParser(record)
    if (parser.parseString(2) != "09") {
        throw ValidationException(
            ValidationFileStatus.UGYLDIG_RECTYPE.code,
            ValidationFileStatus.UGYLDIG_RECTYPE.message
        )
    }
    return EndRecord(
        numberOfRecord = parser.parseInt(9),
        totalBelop = parser.parseAmountAsBigdecimal(12)
    )
}

fun isTransactionRecord(record: String) = "02" == SpkFilParser(record).parseString(2)

fun isEndRecord(record: String) = "09" == SpkFilParser(record).parseString(2)

class SpkFilParser(
    private val record: String
) {
    private var pos = 0
    fun parseString(len: Int): String {
        if (record.length < pos + len) return record.substring(pos).trim()
        return record.substring(pos, pos + len).trim().also { pos += len }
    }

    fun parseInt(len: Int) = this.parseString(len).toInt()

    fun parseInt(value: String): Int =
        try {
            value.toInt()
        } catch (ex: NumberFormatException) {
            -1
        }

    fun parseAmountAsBigdecimal(len: Int) = this.parseString(len).trimStart('0')
        .let {
            BigDecimal(
                when (it.length) {
                    0 -> "0.00"
                    1 -> "0.0$it"
                    2 -> "0.$it"
                    else -> "${it.dropLast(2)}.${it.drop(it.length - 2)}"
                }
            )
        }

    fun parseDate(len: Int): LocalDate = this.parseString(len)
        .let { LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd")) }

    fun parseDate(date: String): LocalDate? =
        try {
            date.let { LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd")) }
        } catch (ex: DateTimeParseException) {
            null
        }
}