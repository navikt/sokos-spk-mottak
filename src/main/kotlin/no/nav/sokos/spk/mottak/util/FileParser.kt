package no.nav.sokos.spk.mottak.util

import java.time.LocalDate

import no.nav.sokos.spk.mottak.domain.Avregningsretur
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.NAV
import no.nav.sokos.spk.mottak.domain.RECTYPE_SLUTTRECORD
import no.nav.sokos.spk.mottak.domain.RECTYPE_STARTRECORD
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus
import no.nav.sokos.spk.mottak.domain.record.SluttRecord
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
import no.nav.sokos.spk.mottak.util.Utils.toLocalDate
import no.nav.sokos.spk.mottak.util.Utils.toLocalDateString

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
            filLopenummer = record.getString(24, 30),
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
            grad = record.getString(93, 96),
            filStatus = filStatus,
        )
    }

    fun createStartRecord(
        anviser: String,
        lopeNr: String,
        filType: String,
        datoMottatt: LocalDate,
        beskrivelse: String,
    ): String {
        val stringBuilder = StringBuilder()
        stringBuilder
            .append(RECTYPE_STARTRECORD)
            .append(NAV.padEnd(11, ' '))
            .append(anviser.padEnd(11, ' '))
            .append(lopeNr.padStart(6, '0'))
            .append(filType.padEnd(3, ' '))
            .append(datoMottatt.toLocalDateString().padEnd(6, ' '))
            .append(beskrivelse.padEnd(35, ' '))
            .append(FilStatus.OK.code)
            .append("".padEnd(35, ' '))
            .appendLine()

        return stringBuilder.toString()
    }

    fun createTransaksjonRecord(innTransaksjon: InnTransaksjon): String {
        val stringBuilder = StringBuilder()
        val transaksjonStatus = TransaksjonStatus.getByCode(innTransaksjon.transaksjonStatus!!)!!
        stringBuilder
            .append(RECTYPE_TRANSAKSJONSRECORD)
            .append(innTransaksjon.transId.padEnd(12, ' '))
            .append(innTransaksjon.fnr.padEnd(11, ' '))
            .append(innTransaksjon.utbetalesTil.orEmpty().padEnd(11, ' '))
            .append(innTransaksjon.datoAnviserStr.padEnd(8, ' '))
            .append(innTransaksjon.datoFomStr.padEnd(8, ' '))
            .append(innTransaksjon.datoTomStr.padEnd(8, ' '))
            .append(innTransaksjon.belopstype.padEnd(2, ' '))
            .append(innTransaksjon.belopStr.padStart(11, '0'))
            .append(innTransaksjon.art.padEnd(4, ' '))
            .append(innTransaksjon.refTransId.orEmpty().padEnd(12, ' '))
            .append(innTransaksjon.tekstkode.orEmpty().padEnd(4, ' '))
            .append(transaksjonStatus.code.padEnd(2, ' '))
            .append(transaksjonStatus.message.padEnd(35, ' ').substring(0, 35))
            .appendLine()

        return stringBuilder.toString()
    }

    fun createAvregningTransaksjonRecord(avregningTransaksjon: Avregningsretur): String {
        val validatedDatoValutert = avregningTransaksjon.datoValutering.takeIf { it != "00000000" } ?: ""
        val stringBuilder =
            StringBuilder()
                .append(RECTYPE_TRANSAKSJONSRECORD)
                .append((avregningTransaksjon.transEksId ?: "").padEnd(12, ' ').substring(0, 12))
                .append(avregningTransaksjon.datoAvsender.toLocalDateString().padEnd(8, ' '))
                .append(avregningTransaksjon.gjelderId.padEnd(11, ' '))
                .append(avregningTransaksjon.utbetalesTil.padEnd(11, ' '))
                .append(avregningTransaksjon.datoStatus.toLocalDateString().padEnd(8, ' '))
                .append(avregningTransaksjon.status.padEnd(4, ' '))
                .append((avregningTransaksjon.statusTekst ?: "").padStart(35, ' ').substring(0, 35))
                .append(avregningTransaksjon.bilagsnrSerie.padEnd(4, ' '))
                .append(avregningTransaksjon.bilagsnr.padEnd(10, ' '))
                .append(avregningTransaksjon.konto.padEnd(9, ' '))
                .append(avregningTransaksjon.datoFom.toLocalDateString().padEnd(8, ' '))
                .append(avregningTransaksjon.datoTom.toLocalDateString().padEnd(8, ' '))
                .append(avregningTransaksjon.belop.padStart(11, '0'))
                .append(avregningTransaksjon.debetKredit.padEnd(1, ' '))
                .append(filterType(avregningTransaksjon.utbetalingtype).padEnd(3, ' '))
                .append(validatedDatoValutert.padEnd(8, ' '))
                .appendLine()

        return stringBuilder.toString()
    }

    private fun filterType(utbetalingtype: String): String {
        return when (utbetalingtype) {
            "UEU" -> "UTK"
            "UIG" -> "UTK"
            else -> utbetalingtype
        }
    }

    fun createSluttRecord(
        antallTransaksjon: Int,
        sumBelop: Long,
    ): String {
        val stringBuilder = StringBuilder()
        stringBuilder
            .append(RECTYPE_SLUTTRECORD)
            .append(antallTransaksjon.toString().padStart(9, '0'))
            .append(sumBelop.toString().padStart(14, '0'))

        return stringBuilder.toString()
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
