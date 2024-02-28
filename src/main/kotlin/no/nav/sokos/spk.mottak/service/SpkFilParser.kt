package no.nav.sokos.spk.mottak.service

import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.modell.FirstLine
import no.nav.sokos.spk.mottak.modell.LastLine
import no.nav.sokos.spk.mottak.modell.Transaksjon
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

fun parseFirsLine(line: String): FirstLine {
    val parser = SpkFilParser(line)
    if (!parser.parseString(2).equals("01") ){
        throw ValidationException(kode = "06", message = "Startrecord er ikke type '01'")
    }

    try {
        return FirstLine(
            avsender = parser.parseString(11),
            mottager = parser.parseString(11),
            filLopenummer = parser.parseInt(6),
            filType = parser.parseString(3),
            produsertDato = parser.parseDate(8),
            beskrivelse = parser.parseString(35),
            filStatus = parser.parseString(2),
            feilTekst = parser.parseString(35),
        )
    } catch (e: DateTimeParseException) {
        throw ValidationException(kode = "09", message = "Validering av produksjonsDato Feilet ved parsing")
    } catch (e: NumberFormatException) {
        throw ValidationException(kode = "04", message = "Validering av løpenummer Feilet ved parsing")
    }
}

fun parseTransactionLine(line: String): Transaksjon {
    val parser = SpkFilParser(line)
    parser.parseString(2)
    return Transaksjon(
        transId = parser.parseString(12),
        gjelderId = parser.parseString(11),
        utbetalesTil = parser.parseString(11),
        datoAnviser = parser.parseDate(8),
        periodeFOM = parser.parseDate(8),
        periodeTOM = parser.parseDate(8),
        belopsType = parser.parseString(2),
        belop = parser.parseAmountAsBigdecimal(9),
        art = parser.parseString(4),
        refTransId = parser.parseString(12),
        tekstKode = parser.parseString(4),
        saldo = parser.parseAmountAsBigdecimal(9),
        prioritet = parser.parseString(8),
        kid = parser.parseString(26),
        trekkansvar = parser.parseString(4),
        grad = parser.parseString(4),
        status = parser.parseString(2),
        feiltekst = parser.parseString(35),
    )
}

fun parseLastLine(line: String): LastLine {
    val parser = SpkFilParser(line)
    if (!parser.parseString(2).equals("09") ){
        throw ValidationException(kode = "06", message = "Sluttrecord er ikke type '09'")
    }
    return LastLine(
        antallLinjer = parser.parseInt(9),
        sumAlleLinjer = parser.parseAmountAsBigdecimal(12),
    )
}

fun erTransaksjon(line: String) = "02".equals(SpkFilParser(line).parseString(2))

fun erLastLine(line: String) = "09".equals(SpkFilParser(line).parseString(2))

class SpkFilParser(
    val line: String
) {
    private var pos = 0
    fun parseString(len: Int): String {
        if (line.length < pos + len) return line.substring(pos).trim()
        return line.substring(pos, pos + len).trim().also { pos += len }
    }

    fun parseInt(len: Int) = this.parseString(len).toInt()

    fun parseAmountAsBigdecimal(len: Int) = this.parseString(len).trimStart('0')
        .let {
            BigDecimal(
                when (it.length) {
                    0 -> "0.00"
                    1 -> "0.0" + it
                    2 -> "0." + it
                    else -> "${it.dropLast(2)}.${it.drop(it.length - 2)}"
                }
            )
        }

    fun parseDate(len: Int) = this.parseString(len)
        .let { LocalDate.parse(it, DateTimeFormatter.ofPattern("yyyyMMdd")) }
}