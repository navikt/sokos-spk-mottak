package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import java.time.LocalDateTime
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.BEHANDLET_NEI
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
import no.nav.sokos.spk.mottak.util.StringUtil.toLocalDate

class InnTransaksjonRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource()
) {
    fun getInnTransaksjoner(filInfoId: Int): List<InnTransaksjon> {
        return sessionOf(dataSource).list(
            queryOf(
                """
                    SELECT * FROM T_INN_TRANSAKSJON WHERE FIL_INFO_ID = :filInfoId
                """.trimIndent(),
                mapOf("filInfoId" to filInfoId)
            ), toInntransaksjon
        )
    }

    fun insertTransactionBatch(transaksjonRecordList: List<TransaksjonRecord>, filInfoId: Long, session: Session) {
        session.batchPreparedNamedStatement(
            """
                INSERT INTO T_INN_TRANSAKSJON (
                FIL_INFO_ID, 
                K_TRANSAKSJON_S,
                FNR_FK,
                BELOPSTYPE,
                ART,
                AVSENDER,
                UTBETALES_TIL,
                DATO_FOM_STR,
                DATO_TOM_STR,
                DATO_ANVISER_STR,
                BELOP_STR,
                REF_TRANS_ID,
                TEKSTKODE,
                RECTYPE,
                TRANS_ID_FK,
                DATO_FOM,
                DATO_TOM,
                DATO_ANVISER,
                BELOP,
                BEHANDLET,
                DATO_OPPRETTET,
                OPPRETTET_AV,
                DATO_ENDRET,
                ENDRET_AV,
                VERSJON,
                PRIORITET_STR,
                TREKKANSVAR,
                SALDO_STR,
                KID,
                PRIORITET,
                SALDO,
                GRAD,
                GRAD_STR) VALUES (:filInfoId, :transaksjonStatus, :fnr, :belopstype, :art, :avsender, :utbetalesTil, :datoFomStr, :datoTomStr, :datoAnviserStr, :belopStr, :refTransId, :tekstkode, :rectype, :transId, :datoFom, :datoTom, :datoAnviser, :belop, :behandlet, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :prioritetStr, :trekkansvar, :saldoStr, :kid, :prioritet, :saldo, :grad, :gradStr)
            """.trimIndent(),
            transaksjonRecordList.convertToListMap(filInfoId)
        )
    }


    private fun List<TransaksjonRecord>.convertToListMap(filInfoId: Long): List<Map<String, Any?>> {
        return this.map { transaksjonRecord ->
            mapOf(
                "filInfoId" to filInfoId,
                "transaksjonStatus" to null,
                "fnr" to transaksjonRecord.fnr,
                "belopstype" to transaksjonRecord.belopstype,
                "art" to transaksjonRecord.art,
                "avsender" to SPK,
                "utbetalesTil" to transaksjonRecord.utbetalesTil,
                "datoFomStr" to transaksjonRecord.datoFom,
                "datoTomStr" to transaksjonRecord.datoTom,
                "datoAnviserStr" to transaksjonRecord.datoAnviser,
                "belopStr" to transaksjonRecord.belop,
                "refTransId" to transaksjonRecord.refTransId,
                "tekstkode" to transaksjonRecord.tekstKode,
                "rectype" to RECTYPE_TRANSAKSJONSRECORD,
                "transId" to transaksjonRecord.transId,
                "datoFom" to transaksjonRecord.datoFom.toLocalDate(),
                "datoTom" to transaksjonRecord.datoTom.toLocalDate(),
                "datoAnviser" to transaksjonRecord.datoAnviser.toLocalDate(),
                "belop" to transaksjonRecord.belop.toIntOrNull(),
                "behandlet" to BEHANDLET_NEI,
                "datoOpprettet" to LocalDateTime.now(),
                "opprettetAv" to PropertiesConfig.Configuration().naisAppName,
                "datoEndret" to LocalDateTime.now(),
                "endretAv" to PropertiesConfig.Configuration().naisAppName,
                "versjon" to "1",
                "prioritetStr" to transaksjonRecord.prioritet,
                "trekkansvar" to transaksjonRecord.trekkansvar,
                "saldoStr" to transaksjonRecord.saldo,
                "kid" to transaksjonRecord.kid,
                "prioritet" to transaksjonRecord.prioritet.toLocalDate(),
                "saldo" to transaksjonRecord.saldo.toIntOrNull(),
                "grad" to transaksjonRecord.grad.toIntOrNull(),
                "gradStr" to transaksjonRecord.grad
            )
        }
    }

    private val toInntransaksjon: (Row) -> InnTransaksjon = { row ->
        InnTransaksjon(
            row.int("INN_TRANSAKSJON_ID"),
            row.int("FIL_INFO_ID"),
            row.stringOrNull("K_TRANSAKSJON_S"),
            row.string("FNR_FK"),
            row.string("BELOPSTYPE"),
            row.string("ART"),
            row.string("AVSENDER"),
            row.string("UTBETALES_TIL"),
            row.string("DATO_FOM_STR"),
            row.string("DATO_TOM_STR"),
            row.string("DATO_ANVISER_STR"),
            row.string("BELOP_STR"),
            row.string("REF_TRANS_ID"),
            row.string("TEKSTKODE"),
            row.string("RECTYPE"),
            row.string("TRANS_ID_FK"),
            row.localDate("DATO_FOM"),
            row.localDate("DATO_TOM"),
            row.localDate("DATO_ANVISER"),
            row.int("BELOP"),
            row.string("BEHANDLET"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON"),
            row.string("PRIORITET_STR"),
            row.string("TREKKANSVAR"),
            row.string("SALDO_STR"),
            row.string("KID"),
            row.localDateOrNull("PRIORITET"),
            row.int("SALDO"),
            row.intOrNull("GRAD"),
            row.string("GRAD_STR")
        )
    }
}

