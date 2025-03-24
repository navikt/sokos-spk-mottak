package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.Avregningsretur
import no.nav.sokos.spk.mottak.domain.SEND_AVREGNINGSRETUR_SERVICE
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.util.SQLUtils.asMap

class AvregningsreturRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) {
    private val insertTimer = Metrics.timer(DATABASE_CALL, "AvregningsreturRepository", "insert")
    private val getByTransaksjonIdTimer =
        Metrics.timer(DATABASE_CALL, "AvregningsreturRepository", "getByTransaksjonId")
    private val getReturTilAnviserWhichIsNotSentTimer =
        Metrics.timer(DATABASE_CALL, "ReturAnviserTransaksjonRepository", "getReturTilAnviserWhichIsNotSent")
    private val updateBatchTimer = Metrics.timer(DATABASE_CALL, "ReturAnviserTransaksjonRepository", "updateBatch")

    fun insert(
        avregningsretur: Avregningsretur,
        session: Session,
    ): Long? =
        insertTimer.recordCallable {
            session.updateAndReturnGeneratedKey(
                queryOf(
                    """
                    INSERT INTO T_RETUR_TIL_ANV (
                    RECTYPE,
                    K_RETUR_T,
                    K_ANVISER,
                    OS_ID_FK,
                    OS_LINJE_ID_FK,
                    TREKKVEDTAK_ID_FK,
                    GJELDER_ID,
                    FNR_FK,
                    DATO_STATUS,
                    STATUS,
                    BILAGSNR_SERIE,
                    BILAGSNR,
                    DATO_FOM,
                    DATO_TOM,
                    BELOP,
                    DEBET_KREDIT,
                    UTBETALING_TYPE,
                    TRANS_TEKST,
                    TRANS_EKS_ID_FK,
                    DATO_AVSENDER,
                    UTBETALES_TIL,
                    STATUS_TEKST,
                    RETURTYPE_KODE,
                    DUPLIKAT,
                    TRANSAKSJON_ID,
                    FIL_INFO_INN_ID,
                    FIL_INFO_UT_ID,
                    DATO_VALUTERING,
                    KONTO,
                    MOT_ID,
                    PERSON_ID,
                    KREDITOR_REF,
                    DATO_OPPRETTET,
                    OPPRETTET_AV,
                    DATO_ENDRET,
                    ENDRET_AV,
                    VERSJON ) VALUES (:rectype, :returtype, :anviser, :osId, :osLinjeId, :trekkvedtakId, :gjelderId, :fnr, :datoStatus, :status, :bilagsnrSerie, :bilagsnr, :datoFom, :datoTom, :belop, :debetKredit, :utbetalingtype, :transTekst, :transEksId, :datoAvsender, :utbetalesTil, :statusTekst, :returtypeKode, :duplikat, :transaksjonId, :filInfoInnId, :filInfoUtId, :datoValutering, :konto, :motId, :personId, :kreditorRef, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon)
                    """.trimIndent(),
                    avregningsretur.asMap(),
                ),
            )
        }

    fun existsByBilagsnr(
        bilagsnrSerie: String,
        bilagsnr: String,
    ): Int? =
        using(sessionOf(dataSource)) { session ->
            getByTransaksjonIdTimer.recordCallable {
                session.single(
                    queryOf(
                        """
                        SELECT EXISTS(
                            SELECT 1
                            FROM T_RETUR_TIL_ANV
                            WHERE BILAGSNR_SERIE = ? AND BILAGSNR = ?
                        ) AS ROW_EXISTS
                        """.trimIndent(),
                        bilagsnrSerie,
                        bilagsnr,
                    ),
                ) { row -> row.int("ROW_EXISTS") }
            }
        }

    fun getReturTilAnviserWhichIsNotSent(): List<Avregningsretur> =
        using(sessionOf(dataSource)) { session ->
            getReturTilAnviserWhichIsNotSentTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT RETUR_TIL_ANV_ID,
                               RECTYPE,
                               K_RETUR_T,
                               K_ANVISER,
                               OS_ID_FK,
                               OS_LINJE_ID_FK,
                               TREKKVEDTAK_ID_FK,
                               GJELDER_ID,
                               FNR_FK,
                               DATO_STATUS,
                               STATUS,
                               BILAGSNR_SERIE,
                               BILAGSNR,
                               DATO_FOM,
                               DATO_TOM,
                               BELOP,
                               DEBET_KREDIT,
                               UTBETALING_TYPE,
                               TRANS_TEKST,
                               TRANS_EKS_ID_FK,
                               DATO_AVSENDER,
                               UTBETALES_TIL,
                               STATUS_TEKST,
                               RETURTYPE_KODE,
                               DUPLIKAT,
                               TRANSAKSJON_ID,
                               FIL_INFO_INN_ID,
                               FIL_INFO_UT_ID,
                               DATO_VALUTERING,
                               KONTO,
                               MOT_ID,
                               PERSON_ID,
                               KREDITOR_REF
                               DATO_OPPRETTET,
                               OPPRETTET_AV,
                               DATO_ENDRET,
                               ENDRET_AV,
                               VERSJON
                        FROM T_RETUR_TIL_ANV
                        WHERE K_RETUR_T = 'AVR' AND K_ANVISER = '$SPK' AND FIL_INFO_UT_ID is null;
                        """.trimIndent(),
                    ),
                    mapToAvregningsretur,
                )
            }
        }

    fun updateBatch(
        returTilAnviserIdList: List<Int>,
        filInfoUtId: Int,
        session: Session,
    ) {
        updateBatchTimer.recordCallable {
            session.batchPreparedNamedStatement(
                """
                UPDATE T_RETUR_TIL_ANV
                    SET FIL_INFO_UT_ID = :filInfoUtId, 
                        DATO_ENDRET = CURRENT_TIMESTAMP, 
                        ENDRET_AV = '$SEND_AVREGNINGSRETUR_SERVICE'
                    WHERE RETUR_TIL_ANV_ID = :returTilAnviserId
                """.trimIndent(),
                returTilAnviserIdList.map { returTilAnviserId ->
                    mapOf(
                        "filInfoUtId" to filInfoUtId,
                        "returTilAnviserId" to returTilAnviserId,
                    )
                },
            )
        }
    }

    // Kun for testing
    fun getByMotId(motId: String): Avregningsretur? =
        using(sessionOf(dataSource)) { session ->
            getByTransaksjonIdTimer.recordCallable {
                session.single(
                    queryOf(
                        """
                        SELECT RETUR_TIL_ANV_ID,
                        RECTYPE,
                        K_RETUR_T,
                        K_ANVISER,
                        OS_ID_FK,
                        OS_LINJE_ID_FK,
                        TREKKVEDTAK_ID_FK,
                        GJELDER_ID,
                        FNR_FK,
                        DATO_STATUS,
                        STATUS,
                        BILAGSNR_SERIE,
                        BILAGSNR,
                        DATO_FOM,
                        DATO_TOM,
                        BELOP,
                        DEBET_KREDIT,
                        UTBETALING_TYPE,
                        TRANS_TEKST,
                        TRANS_EKS_ID_FK,
                        DATO_AVSENDER,
                        UTBETALES_TIL,
                        STATUS_TEKST,
                        RETURTYPE_KODE,
                        DUPLIKAT,
                        TRANSAKSJON_ID,
                        FIL_INFO_INN_ID,
                        FIL_INFO_UT_ID,
                        DATO_VALUTERING,
                        KONTO,
                        MOT_ID,
                        PERSON_ID,
                        KREDITOR_REF,
                        DATO_OPPRETTET,
                        OPPRETTET_AV,
                        DATO_ENDRET,
                        ENDRET_AV,
                        VERSJON
                            FROM T_RETUR_TIL_ANV
                            WHERE MOT_ID = '$motId'
                        """.trimIndent(),
                    ),
                    mapToAvregningsretur,
                )
            }
        }

    // Kun for testing
    fun getByTrekkvedtakId(trekkvedtakId: String): Avregningsretur? =
        using(sessionOf(dataSource)) { session ->
            getByTransaksjonIdTimer.recordCallable {
                session.single(
                    queryOf(
                        """
                        SELECT RETUR_TIL_ANV_ID,
                        RECTYPE,
                        K_RETUR_T,
                        K_ANVISER,
                        OS_ID_FK,
                        OS_LINJE_ID_FK,
                        TREKKVEDTAK_ID_FK,
                        GJELDER_ID,
                        FNR_FK,
                        DATO_STATUS,
                        STATUS,
                        BILAGSNR_SERIE,
                        BILAGSNR,
                        DATO_FOM,
                        DATO_TOM,
                        BELOP,
                        DEBET_KREDIT,
                        UTBETALING_TYPE,
                        TRANS_TEKST,
                        TRANS_EKS_ID_FK,
                        DATO_AVSENDER,
                        UTBETALES_TIL,
                        STATUS_TEKST,
                        RETURTYPE_KODE,
                        DUPLIKAT,
                        TRANSAKSJON_ID,
                        FIL_INFO_INN_ID,
                        FIL_INFO_UT_ID,
                        DATO_VALUTERING,
                        KONTO,
                        MOT_ID,
                        PERSON_ID,
                        KREDITOR_REF,
                        DATO_OPPRETTET,
                        OPPRETTET_AV,
                        DATO_ENDRET,
                        ENDRET_AV,
                        VERSJON
                            FROM T_RETUR_TIL_ANV
                            WHERE TREKKVEDTAK_ID_FK = '$trekkvedtakId';
                        """.trimIndent(),
                    ),
                    mapToAvregningsretur,
                )
            }
        }

    // Kun for testing
    fun getNoOfRows(): Int? =
        using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf("SELECT COUNT(*) FROM T_RETUR_TIL_ANV"),
            ) { row -> row.int(1) }
        }

    private val mapToAvregningsretur: (Row) -> Avregningsretur = { row ->
        Avregningsretur(
            returTilAnviserId = row.int("RETUR_TIL_ANV_ID"),
            rectype = row.string("RECTYPE"),
            returtype = row.string("K_RETUR_T"),
            anviser = row.string("K_ANVISER"),
            osId = row.stringOrNull("OS_ID_FK"),
            osLinjeId = row.stringOrNull("OS_LINJE_ID_FK"),
            trekkvedtakId = row.stringOrNull("TREKKVEDTAK_ID_FK"),
            gjelderId = row.string("GJELDER_ID"),
            fnr = row.stringOrNull("FNR_FK"),
            datoStatus = row.localDate("DATO_STATUS"),
            status = row.string("STATUS"),
            bilagsnrSerie = row.string("BILAGSNR_SERIE"),
            bilagsnr = row.string("BILAGSNR"),
            datoFom = row.localDate("DATO_FOM"),
            datoTom = row.localDate("DATO_TOM"),
            belop = row.string("BELOP"),
            debetKredit = row.string("DEBET_KREDIT"),
            utbetalingtype = row.string("UTBETALING_TYPE"),
            transTekst = row.stringOrNull("TRANS_TEKST"),
            transEksId = row.stringOrNull("TRANS_EKS_ID_FK"),
            datoAvsender = row.localDate("DATO_AVSENDER"),
            utbetalesTil = row.string("UTBETALES_TIL"),
            statusTekst = row.stringOrNull("STATUS_TEKST"),
            returtypeKode = row.stringOrNull("RETURTYPE_KODE"),
            duplikat = row.stringOrNull("DUPLIKAT"),
            transaksjonId = row.intOrNull("TRANSAKSJON_ID"),
            filInfoInnId = row.int("FIL_INFO_INN_ID"),
            filInfoUtId = row.intOrNull("FIL_INFO_UT_ID"),
            datoValutering = row.string("DATO_VALUTERING"),
            konto = row.string("KONTO"),
            motId = row.stringOrNull("MOT_ID"),
            personId = row.stringOrNull("PERSON_ID"),
            kreditorRef = row.stringOrNull("KREDITOR_REF"),
            datoOpprettet = row.localDateTime("DATO_OPPRETTET"),
            opprettetAv = row.string("OPPRETTET_AV"),
            datoEndret = row.localDateTime("DATO_ENDRET"),
            endretAv = row.string("ENDRET_AV"),
            versjon = row.int("VERSJON"),
        )
    }
}
