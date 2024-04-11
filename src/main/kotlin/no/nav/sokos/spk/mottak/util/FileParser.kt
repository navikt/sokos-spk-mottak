package no.nav.sokos.spk.mottak.util

import no.nav.sokos.spk.mottak.domain.RECTYPE_SLUTTRECORD
import no.nav.sokos.spk.mottak.domain.RECTYPE_STARTRECORD
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.util.StringUtil.toLocalDate
import no.nav.sokos.spk.mottak.validator.FileStatus

object FileParser {
    fun parseStartRecord(record: String): StartRecord {
        if (record.getString(0, 2) != RECTYPE_STARTRECORD) {
            throw ValidationException(
                FileStatus.UGYLDIG_RECTYPE.code,
                FileStatus.UGYLDIG_RECTYPE.message
            )
        }

        try {
            return StartRecord(
                avsender = record.getString(2, 13),
                mottager = record.getString(13, 24),
                filLopenummer = record.getString(24, 30).toInt(),
                filType = record.getString(30, 33),
                produsertDato = record.getString(33, 41).toLocalDate()!!,
                beskrivelse = record.getString(41, 75),
                rawRecord = record
            )
        } catch (e: NullPointerException) {
            throw ValidationException(
                FileStatus.UGYLDIG_PRODDATO.code,
                FileStatus.UGYLDIG_PRODDATO.message
            )
        } catch (e: NumberFormatException) {
            throw ValidationException(
                FileStatus.UGYLDIG_FILLOPENUMMER.code,
                FileStatus.UGYLDIG_FILLOPENUMMER.message
            )
        }
    }


    fun parseEndRecord(record: String): EndRecord {
        if (record.getString(0, 2) != RECTYPE_SLUTTRECORD) {
            throw ValidationException(
                FileStatus.UGYLDIG_RECTYPE.code,
                FileStatus.UGYLDIG_RECTYPE.message
            )
        }
        val numberOfRecord: Int
        val totalBelop: Long
        try {
            numberOfRecord = record.getString(2, 11).toInt()
        } catch (e: NumberFormatException) {
            throw ValidationException(
                FileStatus.UGYLDIG_ANTRECORDS.code,
                String.format(FileStatus.UGYLDIG_ANTRECORDS.message, "${record.getString(2, 11)}")
            )
        }
        try {
            totalBelop = record.getString(11, 25).toLong()
        } catch (e: NumberFormatException) {
            throw ValidationException(
                FileStatus.UGYLDIG_SUMBELOP.code,
                String.format(FileStatus.UGYLDIG_SUMBELOP.message, "${record.getString(11, 25)}")
            )
        }
        return EndRecord(
            numberOfRecord = numberOfRecord,
            totalBelop = totalBelop
        )

    }

    fun parseTransaction(record: String): TransaksjonRecord {
        if (record.getString(0, 2) != RECTYPE_TRANSAKSJONSRECORD) {
            throw ValidationException(
                FileStatus.UGYLDIG_RECTYPE.code,
                FileStatus.UGYLDIG_RECTYPE.message
            )
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
            grad = record.getString(142, 146)
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