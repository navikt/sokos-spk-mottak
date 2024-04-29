package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import java.time.LocalDateTime
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.BEHANDLET_JA
import no.nav.sokos.spk.mottak.domain.BEHANDLET_NEI
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_01_UNIK_ID
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_02_GYLDIG_FODSELSNUMMER
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_03_GYLDIG_PERIODE
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_04_GYDLIG_BELOPSTYPE
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_05_UGYLDIG_ART
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_06_GYLDIG_REFERANSE
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_08_GYLDIG_UTBETALES_TIL
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_09_GYLDIG_ANVISER_DATO
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_10_GYLDIG_BELOP
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_11_GYLDIG_KOMBINASJON_ART_OG_BELOPSTYPE
import no.nav.sokos.spk.mottak.util.Util.toLocalDate

private const val READ_ROWS: Int = 10000

class InnTransaksjonRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource()
) {
    fun getByFilInfoId(filInfoId: Int): List<InnTransaksjon> {
        return using(sessionOf(dataSource)) { session ->
            session.list(
                queryOf(
                    """
                    SELECT * FROM T_INN_TRANSAKSJON WHERE FIL_INFO_ID = $filInfoId
                """.trimIndent()
                ), toInntransaksjon
            )
        }
    }

    fun getByTransaksjonStatusIsNullWithPersonId(rows: Int = READ_ROWS): List<InnTransaksjon> {
        return using(sessionOf(dataSource)) { session ->
            session.list(
                queryOf(
                    """
                        SELECT t.*, p.PERSON_ID FROM T_INN_TRANSAKSJON t LEFT OUTER JOIN T_PERSON p ON t.FNR_FK = p.FNR_FK
                        WHERE t.BEHANDLET = 'N'
                        ORDER BY t.FIL_INFO_ID, t.INN_TRANSAKSJON_ID
                        FETCH FIRST $rows ROWS ONLY;
                    """.trimIndent()
                ), toInntransaksjon
            )
        }
    }

    fun validateTransaksjon(session: Session) {
        session.run(queryOf(VALIDATOR_01_UNIK_ID).asUpdate)
        session.run(queryOf(VALIDATOR_02_GYLDIG_FODSELSNUMMER).asUpdate)
        session.run(queryOf(VALIDATOR_03_GYLDIG_PERIODE).asUpdate)
        session.run(queryOf(VALIDATOR_08_GYLDIG_UTBETALES_TIL).asUpdate)
        session.run(queryOf(VALIDATOR_09_GYLDIG_ANVISER_DATO).asUpdate)
        session.run(queryOf(VALIDATOR_10_GYLDIG_BELOP).asUpdate)
        session.run(queryOf(VALIDATOR_04_GYDLIG_BELOPSTYPE).asUpdate)
        session.run(queryOf(VALIDATOR_05_UGYLDIG_ART).asUpdate)
        session.run(queryOf(VALIDATOR_11_GYLDIG_KOMBINASJON_ART_OG_BELOPSTYPE).asUpdate)
        session.run(queryOf(VALIDATOR_06_GYLDIG_REFERANSE).asUpdate)

        session.run(
            queryOf(
                """
                    UPDATE T_INN_TRANSAKSJON SET K_TRANSAKSJON_S='$TRANSAKSJONSTATUS_OK'
                    WHERE K_TRANSAKSJON_S IS NULL                
                """.trimIndent()
            ).asUpdate
        )
    }

    fun insertBatch(transaksjonRecordList: List<TransaksjonRecord>, filInfoId: Long, session: Session) {
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

    fun updateBehandletStatusBatch(innTransaksjonIdList: List<Int>, session: Session) {
        session.batchPreparedNamedStatement(
            """
                UPDATE T_INN_TRANSAKSJON SET BEHANDLET = '$BEHANDLET_JA' WHERE INN_TRANSAKSJON_ID = :innTransaksjonId
            """.trimIndent(),
            innTransaksjonIdList.map { mapOf("innTransaksjonId" to it) }
        )
    }


    private fun List<TransaksjonRecord>.convertToListMap(filInfoId: Long): List<Map<String, Any?>> {
        val systemId = PropertiesConfig.Configuration().naisAppName
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
                "tekstkode" to transaksjonRecord.tekstkode,
                "rectype" to RECTYPE_TRANSAKSJONSRECORD,
                "transId" to transaksjonRecord.transId,
                "datoFom" to transaksjonRecord.datoFom.toLocalDate(),
                "datoTom" to transaksjonRecord.datoTom.toLocalDate(),
                "datoAnviser" to transaksjonRecord.datoAnviser.toLocalDate(),
                "belop" to (transaksjonRecord.belop.toIntOrNull() ?: 0),
                "behandlet" to BEHANDLET_NEI,
                "datoOpprettet" to LocalDateTime.now(),
                "opprettetAv" to systemId,
                "datoEndret" to LocalDateTime.now(),
                "endretAv" to systemId,
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
            row.intOrNull("SALDO"),
            row.intOrNull("GRAD"),
            row.string("GRAD_STR"),
            row.optionalIntOrNull("PERSON_ID")
        )
    }

    private fun Row.optionalIntOrNull(columnLable: String): Int? {
        return runCatching {
            this.intOrNull(columnLable)
        }.getOrNull()
    }
}

