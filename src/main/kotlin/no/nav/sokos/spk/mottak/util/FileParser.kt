package no.nav.sokos.spk.mottak.util

import no.nav.sokos.spk.mottak.domain.RECTYPE_SLUTTRECORD
import no.nav.sokos.spk.mottak.domain.RECTYPE_STARTRECORD
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
import no.nav.sokos.spk.mottak.util.StringUtil.toLocalDate
import no.nav.sokos.spk.mottak.validator.FileStatus

object FileParser {
    fun parseStartRecord(record: String): StartRecord {
        var fileStatus = FileStatus.OK
        if (record.getString(0, 2) != RECTYPE_STARTRECORD) {
            fileStatus = FileStatus.UGYLDIG_RECTYPE
        }  else {
            runCatching {
                record.getString(24, 30).toInt() // filLopenummer
                record.getString(33, 41).toLocalDate()!! // produsertDato
            }.onFailure {
                fileStatus = when (it) {
                    is NumberFormatException -> FileStatus.UGYLDIG_FILLOPENUMMER
                    is NullPointerException -> FileStatus.UGYLDIG_PRODDATO
                    else -> FileStatus.OK
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
            rawRecord = record,
            fileStatus = fileStatus
        )
    }

    fun parseEndRecord(record: String): EndRecord {
        var fileStatus = FileStatus.OK
        if (record.getString(0, 2) != RECTYPE_SLUTTRECORD) {
            fileStatus = FileStatus.UGYLDIG_RECTYPE
        }
        return EndRecord(
            numberOfRecord = record.getString(2, 11).toIntOrNull() ?: 0,
            totalBelop = record.getString(11, 25).toLongOrNull() ?: 0,
            fileStatus = fileStatus
        )
    }

    fun parseTransaction(record: String): TransaksjonRecord {
        var fileStatus = FileStatus.OK
        if (record.getString(0, 2) != RECTYPE_TRANSAKSJONSRECORD) {
            fileStatus = FileStatus.UGYLDIG_RECTYPE
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
            tekstKode = record.getString(89, 93),
            saldo = record.getString(93, 104),
            prioritet = record.getString(104, 112),
            kid = record.getString(112, 138),
            trekkansvar = record.getString(138, 142),
            grad = record.getString(142, 146),
            fileStatus = fileStatus
        )
    }

    private fun String.getString(start: Int, end: Int): String {
        return runCatching {
            if (this.length >= end) {
                return this.substring(start, end).trim()
            }
            return this.substring(start, this.length).trim()
        }.getOrDefault("")
    }
}
