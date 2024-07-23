package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
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
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_01_UNIK_ID
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_02_GYLDIG_FODSELSNUMMER
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_03_GYLDIG_PERIODE
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_04_GYDLIG_BELOPSTYPE
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_05_UGYLDIG_ART
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_09_GYLDIG_ANVISER_DATO
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_10_GYLDIG_BELOP
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_11_GYLDIG_KOMBINASJON_ART_OG_BELOPSTYPE
import no.nav.sokos.spk.mottak.repository.TransaksjonValidatorQuery.VALIDATOR_16_GYLDIG_GRAD
import no.nav.sokos.spk.mottak.util.SQLUtils.optionalOrNull
import no.nav.sokos.spk.mottak.util.Utils.toLocalDate
import java.time.LocalDateTime

private const val READ_ROWS: Int = 10000

class InnTransaksjonRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    private val getByFilInfoIdTimer = Metrics.timer(DATABASE_CALL, "InnTransaksjonRepository", "getByFilInfoId")
    private val getByBehandletTimer = Metrics.timer(DATABASE_CALL, "InnTransaksjonRepository", "getByBehandlet")
    private val validateTransaksjonTimer = Metrics.timer(DATABASE_CALL, "InnTransaksjonRepository", "validateTransaksjon")
    private val insertBatchTimer = Metrics.timer(DATABASE_CALL, "InnTransaksjonRepository", "insertBatch")
    private val updateBehandletStatusBatchTimer = Metrics.timer(DATABASE_CALL, "InnTransaksjonRepository", "updateBehandletStatusBatch")
    private val deleteByFilInfoIdTimer = Metrics.timer(DATABASE_CALL, "InnTransaksjonRepository", "deleteByFilInfoId")
    private val findLastFagomraadeByPersonIdTimer = Metrics.timer(DATABASE_CALL, "InnTransaksjonRepository", "findLastFagomraadeByPersonId")

    fun getByFilInfoId(filInfoId: Int): List<InnTransaksjon> =
        using(sessionOf(dataSource)) { session ->
            getByFilInfoIdTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT INN_TRANSAKSJON_ID, FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM,
                            DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, PRIORITET_STR, TREKKANSVAR, SALDO_STR, KID, PRIORITET, SALDO, GRAD, GRAD_STR
                        FROM T_INN_TRANSAKSJON 
                        WHERE FIL_INFO_ID = $filInfoId AND AVSENDER = '$SPK'
                        ORDER BY INN_TRANSAKSJON_ID;
                        """.trimIndent(),
                    ),
                    mapToInntransaksjon,
                )
            }
        }

    fun getByBehandlet(
        behandlet: String = BEHANDLET_NEI,
        rows: Int = READ_ROWS,
    ): List<InnTransaksjon> =
        using(sessionOf(dataSource)) { session ->
            getByBehandletTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT t.INN_TRANSAKSJON_ID, t.FIL_INFO_ID, t.K_TRANSAKSJON_S, t.FNR_FK, t.BELOPSTYPE, ART, t.AVSENDER, t.UTBETALES_TIL, t.DATO_FOM_STR, t.DATO_TOM_STR, t.DATO_ANVISER_STR, t.BELOP_STR, t.REF_TRANS_ID, t.TEKSTKODE, t.RECTYPE, t.TRANS_ID_FK, t.DATO_FOM, t.DATO_TOM,
                            t.DATO_ANVISER, t.BELOP, BEHANDLET, t.DATO_OPPRETTET, t.OPPRETTET_AV, t.DATO_ENDRET, t.ENDRET_AV, t.VERSJON, t.PRIORITET_STR, t.TREKKANSVAR, t.SALDO_STR, t.KID, t.PRIORITET, t.SALDO, t.GRAD, t.GRAD_STR, p.PERSON_ID 
                        FROM T_INN_TRANSAKSJON t LEFT OUTER JOIN T_PERSON p ON t.FNR_FK = p.FNR_FK
                        WHERE t.BEHANDLET = '$behandlet' AND AVSENDER = '$SPK'
                        ORDER BY t.FIL_INFO_ID, t.INN_TRANSAKSJON_ID
                        FETCH FIRST $rows ROWS ONLY;
                        """.trimIndent(),
                    ),
                    mapToInntransaksjon,
                )
            }
        }

    fun validateTransaksjon(session: Session) {
        validateTransaksjonTimer.recordCallable {
            session.update(queryOf(VALIDATOR_01_UNIK_ID))
            session.update(queryOf(VALIDATOR_02_GYLDIG_FODSELSNUMMER))
            session.update(queryOf(VALIDATOR_03_GYLDIG_PERIODE))
            session.update(queryOf(VALIDATOR_09_GYLDIG_ANVISER_DATO))
            session.update(queryOf(VALIDATOR_10_GYLDIG_BELOP))
            session.update(queryOf(VALIDATOR_04_GYDLIG_BELOPSTYPE))
            session.update(queryOf(VALIDATOR_05_UGYLDIG_ART))
            session.update(queryOf(VALIDATOR_11_GYLDIG_KOMBINASJON_ART_OG_BELOPSTYPE))
            session.update(queryOf(VALIDATOR_16_GYLDIG_GRAD))
            session.update(
                queryOf(
                    """
                    UPDATE T_INN_TRANSAKSJON SET K_TRANSAKSJON_S='$TRANSAKSJONSTATUS_OK'
                    WHERE K_TRANSAKSJON_S IS NULL                
                    """.trimIndent(),
                ),
            )
        }
    }

    fun insertBatch(
        transaksjonRecordList: List<TransaksjonRecord>,
        filInfoId: Long,
        session: Session,
    ): List<Int> =
        insertBatchTimer
            .recordCallable {
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
                    GRAD,
                    GRAD_STR) VALUES (:filInfoId, :transaksjonStatus, :fnr, :belopstype, :art, :avsender, :utbetalesTil, :datoFomStr, :datoTomStr, :datoAnviserStr, :belopStr, :refTransId, :tekstkode, :rectype, :transId, :datoFom, :datoTom, :datoAnviser, :belop, :behandlet, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :grad, :gradStr)
                    """.trimIndent(),
                    transaksjonRecordList.convertToListMap(filInfoId),
                )
            }.orEmpty()

    fun updateBehandletStatusBatch(
        innTransaksjonIdList: List<Int>,
        behandlet: String = BEHANDLET_JA,
        session: Session,
    ) {
        updateBehandletStatusBatchTimer.recordCallable {
            session.batchPreparedNamedStatement(
                """
                UPDATE T_INN_TRANSAKSJON SET BEHANDLET = '$behandlet' WHERE INN_TRANSAKSJON_ID = :innTransaksjonId
                """.trimIndent(),
                innTransaksjonIdList.map { mapOf("innTransaksjonId" to it) },
            )
        }
    }

    fun deleteByFilInfoId(
        filInfoId: Int,
        session: Session,
    ) {
        deleteByFilInfoIdTimer.recordCallable {
            session.execute(
                queryOf(
                    """
                    DELETE FROM T_INN_TRANSAKSJON WHERE FIL_INFO_ID = :filInfoId
                    """.trimIndent(),
                    mapOf("filInfoId" to filInfoId),
                ),
            )
        }
    }

    fun findLastFagomraadeByPersonId(personIdListe: List<Int>): Map<Int, String> {
        val fagomradeMap = mutableMapOf<Int, String>()
        using(sessionOf(dataSource)) { session ->
            findLastFagomraadeByPersonIdTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT DISTINCT inn.INN_TRANSAKSJON_ID, g.K_FAGOMRADE
                        FROM T_INN_TRANSAKSJON inn
                            INNER JOIN T_PERSON p ON inn.FNR_FK = p.FNR_FK
                            INNER JOIN T_K_GYLDIG_KOMBIN g ON g.K_ART = inn.ART AND g.K_BELOP_T = inn.BELOPSTYPE AND g.K_ANVISER = '$SPK'
                        WHERE p.person_Id IN (${personIdListe.joinToString()})
                        AND g.K_FAGOMRADE IN 
                            (SELECT DISTINCT g.K_FAGOMRADE
                            FROM T_TRANSAKSJON t
                                INNER JOIN T_K_GYLDIG_KOMBIN g ON g.K_ART = t.K_ART AND g.K_BELOP_T = t.K_BELOP_T
                            WHERE t.person_Id = p.PERSON_ID
                            AND t.K_ANVISER = '$SPK')
                        """.trimIndent(),
                    ),
                ) { row -> fagomradeMap[row.int("INN_TRANSAKSJON_ID")] = row.string("K_FAGOMRADE") }
            }
        }
        return fagomradeMap
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
                "utbetalesTil" to transaksjonRecord.utbetalesTil.trim().ifBlank { null },
                "datoFomStr" to transaksjonRecord.datoFom,
                "datoTomStr" to transaksjonRecord.datoTom,
                "datoAnviserStr" to transaksjonRecord.datoAnviser,
                "belopStr" to transaksjonRecord.belop,
                "refTransId" to transaksjonRecord.refTransId.ifBlank { null },
                "tekstkode" to transaksjonRecord.tekstkode.ifBlank { null },
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
                "grad" to transaksjonRecord.grad.toIntOrNull(),
                "gradStr" to transaksjonRecord.grad.trim().ifBlank { null },
            )
        }
    }

    private val mapToInntransaksjon: (Row) -> InnTransaksjon = { row ->
        InnTransaksjon(
            row.int("INN_TRANSAKSJON_ID"),
            row.int("FIL_INFO_ID"),
            row.stringOrNull("K_TRANSAKSJON_S"),
            row.string("FNR_FK"),
            row.string("BELOPSTYPE"),
            row.string("ART"),
            row.string("AVSENDER"),
            row.stringOrNull("UTBETALES_TIL"),
            row.string("DATO_FOM_STR"),
            row.string("DATO_TOM_STR"),
            row.string("DATO_ANVISER_STR"),
            row.string("BELOP_STR"),
            row.stringOrNull("REF_TRANS_ID"),
            row.stringOrNull("TEKSTKODE"),
            row.string("RECTYPE"),
            row.string("TRANS_ID_FK"),
            row.localDateOrNull("DATO_FOM"),
            row.localDateOrNull("DATO_TOM"),
            row.localDateOrNull("DATO_ANVISER"),
            row.int("BELOP"),
            row.string("BEHANDLET"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON"),
            row.intOrNull("GRAD"),
            row.stringOrNull("GRAD_STR"),
            row.optionalOrNull("PERSON_ID"),
        )
    }
}
