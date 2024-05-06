package no.nav.sokos.spk.mottak.util

import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.RECTYPE_SLUTTRECORD
import no.nav.sokos.spk.mottak.domain.RECTYPE_STARTRECORD
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.record.SluttRecord
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
import no.nav.sokos.spk.mottak.util.Util.toLocalDate

object FileParser {
    fun parseStartRecord(record: String): StartRecord {
        var filStatus = FilStatus.OK
        if (record.getString(0, 2) != RECTYPE_STARTRECORD) {
            filStatus = FilStatus.UGYLDIG_RECTYPE
        } else {
            runCatching {
                record.getString(24, 30).toInt() // filLopenummer
                record.getString(33, 41).toLocalDate()!! // produsertDato
            }.onFailure {
                filStatus =
                    when (it) {
                        is NumberFormatException -> FilStatus.UGYLDIG_FILLOPENUMMER
                        is NullPointerException -> FilStatus.UGYLDIG_PRODDATO
                        else -> FilStatus.OK
                    }
            }
        }
        return StartRecord(
            avsender = record.getString(2, 13),
            mottager = record.getString(13, 24),
            filLopenummer = record.getString(24, 30).toIntOrNull() ?: 0,
            filType = record.getString(30, 33),
            produsertDato = record.getString(33, 41).toLocalDate(),
            beskrivelse = record.getString(41, 75),
            kildeData = record,
            filStatus = filStatus,
        )
    }

    fun parseSluttRecord(record: String): SluttRecord {
        var filStatus = FilStatus.OK
        if (record.getString(0, 2) != RECTYPE_SLUTTRECORD) {
            filStatus = FilStatus.UGYLDIG_RECTYPE
        }
        return SluttRecord(
            antallRecord = record.getString(2, 11).toIntOrNull() ?: 0,
            totalBelop = record.getString(11, 25).toLongOrNull() ?: 0,
            filStatus = filStatus,
        )
    }

    fun parseTransaksjonRecord(record: String): TransaksjonRecord {
        var filStatus = FilStatus.OK
        if (record.getString(0, 2) != RECTYPE_TRANSAKSJONSRECORD) {
            filStatus = FilStatus.UGYLDIG_RECTYPE
        }
        return TransaksjonRecord(
            transId = record.getString(2, 14).trim(),
            fnr = record.getString(14, 25),
            utbetalesTil = record.getString(25, 36).trim(),
            datoAnviser = record.getString(36, 44),
            datoFom = record.getString(44, 52),
            datoTom = record.getString(52, 60),
            belopstype = record.getString(60, 62),
            belop = record.getString(62, 73),
            art = record.getString(73, 77),
            refTransId = record.getString(77, 89),
            tekstkode = record.getString(89, 93),
            saldo = record.getString(93, 104),
            prioritet = record.getString(104, 112),
            kid = record.getString(112, 138),
            trekkansvar = record.getString(138, 142),
            grad = record.getString(142, 146),
            filStatus = filStatus,
        )
    }

    private fun String.getString(
        start: Int,
        end: Int,
    ): String {
        return runCatching {
            if (this.length >= end) {
                return this.substring(start, end).trim()
            }
            return this.substring(start, this.length).trim()
        }.getOrDefault("")
    }
}
