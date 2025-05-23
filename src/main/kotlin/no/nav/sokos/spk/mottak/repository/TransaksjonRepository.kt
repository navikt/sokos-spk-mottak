package no.nav.sokos.spk.mottak.repository

import java.time.LocalDate

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.GyldigKombinasjon
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY_EKSIST
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.TransaksjonDetalj
import no.nav.sokos.spk.mottak.domain.TransaksjonOppsummering
import no.nav.sokos.spk.mottak.domain.VALIDATE_TRANSAKSJON_SERVICE
import no.nav.sokos.spk.mottak.dto.Avregningstransaksjon
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.util.SQLUtils.asMap
import no.nav.sokos.spk.mottak.util.SQLUtils.optionalOrNull
import no.nav.sokos.spk.mottak.util.Utils.booleanToString

class TransaksjonRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) {
    private val findTransaksjonDetaljerByFilInfoIdTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "findTransaksjonDetaljerByFilInfoId")
    private val findTransaksjonOppsummeringByFilInfoIdTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "findTransaksjonOppsummeringByFilInfoId")
    private val insertBatchTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "insertBatch")
    private val updateAllWhereTranstolkningIsNyForMoreThanOneInstanceTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "updateAllWhereTranstolkningIsNyForMoreThanOneInstance")
    private val getAllPersonIdWhereTranstolkningIsNyForMoreThanOneInstanceTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "getAllPersonIdWhereTranstolkningIsNyForMoreThanOneInstance")
    private val updateTransTilstandIdTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "updateTransTilstandId")
    private val findLastTransaksjonByPersonIdTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "findLastTransaksjonByPersonId")
    private val findAllByBelopstypeAndByTransaksjonTilstandTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "findAllByBelopstypeAndByTransaksjonTilstand")
    private val updateBatchTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "updateTransTilstandStatus")
    private val findTransaksjonByMotIdAndTomDatoTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "findTransaksjonByMotIdAndPersonIdAndTomDato")
    private val findTransaksjonByTrekkvedtakIdTimer = Metrics.timer(DATABASE_CALL, "TransaksjonRepository", "findTransaksjonByTrekkvedtakId")

    fun insertBatch(
        transaksjonList: List<Transaksjon>,
        session: Session,
    ) {
        insertBatchTimer.recordCallable {
            session.batchPreparedNamedStatement(
                """
                INSERT INTO T_TRANSAKSJON  (
                    TRANSAKSJON_ID,
                    FIL_INFO_ID, 
                    K_TRANSAKSJON_S, 
                    PERSON_ID, 
                    K_BELOP_T, 
                    K_ART, 
                    K_ANVISER, 
                    FNR_FK, 
                    UTBETALES_TIL, 
                    DATO_FOM, 
                    DATO_TOM, 
                    DATO_ANVISER, 
                    DATO_PERSON_FOM, 
                    DATO_REAK_FOM, 
                    BELOP, 
                    REF_TRANS_ID, 
                    TEKSTKODE, 
                    RECTYPE, 
                    TRANS_EKS_ID_FK, 
                    K_TRANS_TOLKNING, 
                    SENDT_TIL_OPPDRAG,
                    FNR_ENDRET, 
                    MOT_ID, 
                    DATO_OPPRETTET, 
                    OPPRETTET_AV, 
                    DATO_ENDRET, 
                    ENDRET_AV, 
                    VERSJON, 
                    K_TRANS_TILST_T,
                    GRAD
                ) VALUES (:transaksjonId, :filInfoId, :transaksjonStatus, :personId, :belopstype, :art, :anviser, :fnr, :utbetalesTil, :datoFom, :datoTom, :datoAnviser, :datoPersonFom, :datoReakFom, :belop, :refTransId, :tekstkode, :rectype, :transEksId, :transTolkning, :sendtTilOppdrag, :fnrEndret, :motId, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :transTilstandType, :grad)
                """.trimIndent(),
                transaksjonList.map { it.asMap() },
            )
        }
    }

    fun updateBatch(
        transaksjonIdList: List<Int>,
        transTilstandIdList: List<Int>,
        transaksjonTilstandType: String,
        systemId: String,
        vedtaksId: String? = null,
        osStatus: String? = null,
        session: Session,
    ) {
        updateBatchTimer.recordCallable {
            session.batchPreparedNamedStatement(
                """
                UPDATE T_TRANSAKSJON 
                    SET K_TRANS_TILST_T = :transaksjonTilstandType,
                        TREKKVEDTAK_ID_FK = :vedtaksId, 
                        OS_STATUS = :osStatus,
                        ENDRET_AV = :systemId,
                        DATO_ENDRET = CURRENT_TIMESTAMP,
                        TRANS_TILSTAND_ID = :transTilstandId                        
                    WHERE TRANSAKSJON_ID = :transaksjonId;
                """.trimIndent(),
                transaksjonIdList.zip(transTilstandIdList).map { it ->
                    mapOf(
                        "transaksjonId" to it.first,
                        "transTilstandId" to it.second,
                        "transaksjonTilstandType" to transaksjonTilstandType,
                        "vedtaksId" to (vedtaksId?.trimStart { it == '0' }),
                        "osStatus" to osStatus,
                        "systemId" to systemId,
                    )
                },
            )
        }
    }

    fun updateAllWhereTranstolkningIsNyForMoreThanOneInstance(
        personIdListe: List<Int>,
        session: Session,
    ) {
        updateAllWhereTranstolkningIsNyForMoreThanOneInstanceTimer.recordCallable {
            session.update(
                queryOf(
                    """
                    UPDATE T_TRANSAKSJON
                    SET K_TRANS_TOLKNING = '$TRANS_TOLKNING_NY_EKSIST', ENDRET_AV = '$VALIDATE_TRANSAKSJON_SERVICE', DATO_ENDRET = CURRENT_TIMESTAMP
                    WHERE TRANSAKSJON_ID IN (SELECT DISTINCT t1.TRANSAKSJON_ID
                                             FROM T_TRANSAKSJON t1
                                                      INNER JOIN T_K_GYLDIG_KOMBIN k1 on (t1.K_ART = k1.K_ART AND t1.K_BELOP_T = k1.K_BELOP_T AND k1.ER_GYLDIG = '1')
                                                      INNER JOIN T_TRANSAKSJON t2 ON (t1.PERSON_ID = t2.PERSON_ID AND t1.FIL_INFO_ID = t2.FIL_INFO_ID)
                                                      INNER JOIN T_K_GYLDIG_KOMBIN k2 on (t2.K_ART = k2.K_ART AND t2.K_BELOP_T = k2.K_BELOP_T AND k2.ER_GYLDIG = '1')
                                             WHERE t1.K_TRANS_TOLKNING = '$TRANS_TOLKNING_NY'
                                               AND t1.K_TRANS_TILST_T = '$TRANS_TILSTAND_OPPRETTET'
                                               AND t1.K_ANVISER = '$SPK'
                                               AND t1.TRANSAKSJON_ID > t2.TRANSAKSJON_ID
                                               AND k1.K_FAGOMRADE = k2.K_FAGOMRADE
                                               AND t1.PERSON_ID IN (${personIdListe.joinToString()})
                                             ORDER BY t1.TRANSAKSJON_ID);
                    """.trimIndent(),
                ),
            )
        }
    }

    fun getAllPersonIdWhereTranstolkningIsNyForMoreThanOneInstance(): Map<Int, List<String>> =
        using(sessionOf(dataSource)) { session ->
            getAllPersonIdWhereTranstolkningIsNyForMoreThanOneInstanceTimer.recordCallable {
                val personIdMap = mutableMapOf<Int, MutableList<String>>()

                session.list(
                    queryOf(
                        """
                        SELECT DISTINCT t.PERSON_ID, k.K_FAGOMRADE
                        FROM T_TRANSAKSJON t
                                 INNER JOIN T_K_GYLDIG_KOMBIN k on (t.K_ART = k.K_ART AND t.K_BELOP_T = k.K_BELOP_T AND t.K_ANVISER = k.K_ANVISER AND k.ER_GYLDIG = '1')
                        WHERE t.K_BELOP_T IN ('01', '02')
                          AND t.K_TRANS_TOLKNING = '$TRANS_TOLKNING_NY'
                          AND t.K_TRANS_TILST_T = '$TRANS_TILSTAND_OPPRETTET'
                          AND t.K_ANVISER = '$SPK' 
                        GROUP BY t.PERSON_ID, k.K_FAGOMRADE
                        HAVING COUNT(*) > 1
                        """.trimIndent(),
                    ),
                ) { row -> personIdMap.computeIfAbsent(row.int("PERSON_ID")) { mutableListOf() }.add(row.string("K_FAGOMRADE")) }
                personIdMap
            }
        }

    fun updateTransTilstandId(session: Session) {
        updateTransTilstandIdTimer.recordCallable {
            session.update(
                queryOf(
                    """
                    UPDATE T_TRANSAKSJON t
                        SET TRANS_TILSTAND_ID = (SELECT tt.TRANS_TILSTAND_ID
                                                 FROM T_TRANS_TILSTAND tt WHERE t.TRANSAKSJON_ID = tt.TRANSAKSJON_ID),
                            ENDRET_AV = '$VALIDATE_TRANSAKSJON_SERVICE',
                            DATO_ENDRET = CURRENT_TIMESTAMP
                        WHERE t.TRANS_TILSTAND_ID IS NULL AND t.K_ANVISER = '$SPK';
                    """.trimIndent(),
                ),
            )
        }
    }

    fun findLastTransaksjonByPersonId(personIdListe: List<Int>): List<Transaksjon> =
        using(sessionOf(dataSource)) { session ->
            findLastTransaksjonByPersonIdTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        WITH LatestTransaksjon AS (
                            SELECT t.PERSON_ID, MAX(t.DATO_TOM) AS LatestDatoTom
                            FROM T_TRANSAKSJON t
                            WHERE t.K_BELOP_T IN ('01', '02')
                              AND t.K_ANVISER = 'SPK'
                            GROUP BY t.PERSON_ID
                        )
                        SELECT t.*
                        FROM T_INN_TRANSAKSJON inn
                                 INNER JOIN T_PERSON p ON inn.FNR_FK = p.FNR_FK
                                 INNER JOIN T_TRANSAKSJON t ON t.PERSON_ID = p.PERSON_ID
                                 INNER JOIN LatestTransaksjon lt ON t.PERSON_ID = lt.PERSON_ID AND t.DATO_TOM = lt.LatestDatoTom
                        WHERE p.PERSON_ID IN (${personIdListe.joinToString()}) 
                          AND inn.BELOPSTYPE IN ('01', '02')
                          AND t.K_ANVISER = '$SPK';
                        """.trimIndent(),
                    ),
                    mapToTransaksjon,
                )
            }
        }

    fun findAllByBelopstypeAndByTransaksjonTilstand(
        belopstype: List<String>,
        transaksjonTilstand: List<String>,
    ): List<Transaksjon> =
        using(sessionOf(dataSource)) { session ->
            findAllByBelopstypeAndByTransaksjonTilstandTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT t.TRANSAKSJON_ID, t.TRANS_TILSTAND_ID, t.FIL_INFO_ID, t.K_TRANSAKSJON_S, t.PERSON_ID, t.K_BELOP_T, t.K_ART, t.K_ANVISER, t.FNR_FK, t.UTBETALES_TIL, t.OS_ID_FK, t.OS_LINJE_ID_FK, t.DATO_FOM, t.DATO_TOM, t.DATO_ANVISER, t.DATO_PERSON_FOM, t.DATO_REAK_FOM, t.BELOP,
                               t.REF_TRANS_ID, t.TEKSTKODE, RECTYPE, t.TRANS_EKS_ID_FK, t.K_TRANS_TOLKNING, t.SENDT_TIL_OPPDRAG, t.TREKKVEDTAK_ID_FK, t.FNR_ENDRET, t.MOT_ID, t.OS_STATUS, t.DATO_OPPRETTET, t.OPPRETTET_AV, t.DATO_ENDRET, t.ENDRET_AV, t.VERSJON, t.SALDO, t.KID, t.PRIORITET,
                               t.K_TREKKANSVAR, t.K_TRANS_TILST_T, t.GRAD, k.K_FAGOMRADE, k.OS_KLASSIFIKASJON, k.K_TREKKGRUPPE, k.K_TREKK_T, k.K_TREKKALT_T
                        FROM T_TRANSAKSJON t
                            INNER JOIN T_K_GYLDIG_KOMBIN k on (t.K_ART = k.K_ART AND t.K_BELOP_T = k.K_BELOP_T AND t.K_ANVISER = k.K_ANVISER AND k.ER_GYLDIG = '1')
                        WHERE t.K_ANVISER = '$SPK' 
                        AND k.ER_GYLDIG = 1
                        AND t.K_BELOP_T IN (${belopstype.joinToString(separator = "','", prefix = "'", postfix = "'")}) 
                        AND t.K_TRANS_TILST_T IN (${transaksjonTilstand.joinToString(separator = "','", prefix = "'", postfix = "'")})
                        ORDER BY t.PERSON_ID, t.TRANSAKSJON_ID, t.K_ART, t.K_BELOP_T
                        """.trimIndent(),
                    ),
                    mapToTransaksjon,
                )
            }
        }

    fun findTransaksjonOppsummeringByFilInfoId(filInfoId: Int): List<TransaksjonOppsummering> =
        using(sessionOf(dataSource)) { session ->
            findTransaksjonOppsummeringByFilInfoIdTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT count(*) AS ANTALL, t.PERSON_ID, k.K_FAGOMRADE, t.FIL_INFO_ID, t.OS_STATUS, t.K_TRANS_TILST_T, SUM(CAST(t.BELOP/100 AS BIGINT)) AS BELOP
                        FROM T_TRANSAKSJON t
                                 INNER JOIN T_K_GYLDIG_KOMBIN k ON (k.K_ART = t.K_ART and k.K_BELOP_T = t.K_BELOP_T and k.ER_GYLDIG = '1')
                        WHERE k.K_FAGOMRADE IN ('PENSPK', 'UFORESPK', 'SPKBP') AND t.FIL_INFO_ID = $filInfoId
                        group by t.PERSON_ID, k.K_FAGOMRADE, t.FIL_INFO_ID, t.K_TRANS_TILST_T, t.OS_STATUS
                        """.trimIndent(),
                    ),
                ) { row ->
                    TransaksjonOppsummering(
                        row.int("PERSON_ID"),
                        row.string("K_FAGOMRADE"),
                        row.int("FIL_INFO_ID"),
                        row.intOrNull("OS_STATUS"),
                        row.string("K_TRANS_TILST_T"),
                        row.int("ANTALL"),
                        row.bigDecimal("BELOP"),
                    )
                }
            }
        }

    fun findTransaksjonDetaljerByFilInfoId(filInfoIdList: List<Int>): List<TransaksjonDetalj> =
        using(sessionOf(dataSource)) { session ->
            findTransaksjonDetaljerByFilInfoIdTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT DISTINCT t.TRANSAKSJON_ID, t.FNR_FK, k.K_FAGOMRADE, t.OS_STATUS, t.K_TRANS_TILST_T, tt.FEILKODE, tt.FEILKODEMELDING, t.DATO_OPPRETTET
                        FROM T_TRANSAKSJON t 
                                    INNER JOIN T_K_GYLDIG_KOMBIN k ON (k.K_ART = t.K_ART AND k.K_BELOP_T = t.K_BELOP_T AND t.K_BELOP_T IN ('01', '02') AND k.ER_GYLDIG = '1')
                                    INNER JOIN T_TRANS_TILSTAND tt ON t.TRANSAKSJON_ID = tt.TRANSAKSJON_ID AND tt.TRANS_TILSTAND_ID = (
                                        SELECT MAX(TRANS_TILSTAND_ID)
                                        FROM T_TRANS_TILSTAND 
                                        WHERE TRANSAKSJON_ID = t.TRANSAKSJON_ID
                                    )
                        WHERE k.K_ANVISER = '$SPK' AND t.FIL_INFO_ID IN (${filInfoIdList.joinToString()}) 
                        AND ((tt.FEILKODE <> '' AND t.OS_STATUS <> '00') OR (t.K_TRANS_TILST_T = 'OSO' AND t.OS_STATUS is null))
                        ORDER BY t.TRANSAKSJON_ID;
                        """.trimIndent(),
                    ),
                ) { row ->
                    TransaksjonDetalj(
                        row.int("TRANSAKSJON_ID"),
                        row.string("FNR_FK"),
                        row.string("K_FAGOMRADE"),
                        row.stringOrNull("OS_STATUS"),
                        row.string("K_TRANS_TILST_T"),
                        row.stringOrNull("FEILKODE"),
                        row.stringOrNull("FEILKODEMELDING"),
                        row.localDateTime("DATO_OPPRETTET"),
                    )
                }
            }
        }

    fun findTransaksjonByMotIdAndPersonIdAndTomDato(
        motId: String,
        personId: Int,
        tomDato: LocalDate,
    ): Avregningstransaksjon? =
        using(sessionOf(dataSource)) { session ->
            findTransaksjonByMotIdAndTomDatoTimer.recordCallable {
                session.single(
                    queryOf(
                        """
                        SELECT t.TRANSAKSJON_ID,
                        t.FNR_FK,
                        t.TRANS_EKS_ID_FK,
                        t.DATO_ANVISER
                                FROM T_TRANSAKSJON t
                                WHERE t.MOT_ID = '$motId'
                                AND t.PERSON_ID = $personId
                                AND t.DATO_TOM = '$tomDato'
                        """.trimIndent(),
                    ),
                    mapToAvregningstransaksjon,
                )
            }
        }

    fun findTransaksjonByTrekkvedtakId(trekkvedtakId: Int): Avregningstransaksjon? =
        using(sessionOf(dataSource)) { session ->
            findTransaksjonByTrekkvedtakIdTimer.recordCallable {
                session.single(
                    queryOf(
                        """
                        SELECT t.TRANSAKSJON_ID,
                        t.FNR_FK,
                        t.TRANS_EKS_ID_FK,
                        t.DATO_ANVISER
                                FROM T_TRANSAKSJON t
                                WHERE t.TREKKVEDTAK_ID_FK = '$trekkvedtakId'
                        """.trimIndent(),
                    ),
                    mapToAvregningstransaksjon,
                )
            }
        }

    /** Bruker kun for testing */
    fun getByTransaksjonId(transaksjonId: Int): Transaksjon? =
        using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                    SELECT TRANSAKSJON_ID,
                    TRANS_TILSTAND_ID,
                    FIL_INFO_ID,
                    K_TRANSAKSJON_S,
                    PERSON_ID,
                    K_BELOP_T,
                    K_ART,
                    K_ANVISER,
                    FNR_FK,
                    UTBETALES_TIL,
                    OS_ID_FK,
                    OS_LINJE_ID_FK,
                    DATO_FOM,
                    DATO_TOM,
                    DATO_ANVISER,
                    DATO_PERSON_FOM,
                    DATO_REAK_FOM,
                    BELOP,
                    REF_TRANS_ID,
                    TEKSTKODE,
                    RECTYPE,
                    TRANS_EKS_ID_FK,
                    K_TRANS_TOLKNING,
                    SENDT_TIL_OPPDRAG,
                    TREKKVEDTAK_ID_FK,
                    FNR_ENDRET,
                    MOT_ID,
                    OS_STATUS,
                    DATO_OPPRETTET,
                    OPPRETTET_AV,
                    DATO_ENDRET,
                    ENDRET_AV,
                    VERSJON,
                    SALDO,
                    KID,
                    PRIORITET,
                    K_TREKKANSVAR,
                    K_TRANS_TILST_T,
                    GRAD
                            FROM T_TRANSAKSJON
                            WHERE TRANSAKSJON_ID = $transaksjonId
                    """.trimIndent(),
                ),
                mapToTransaksjon,
            )
        }

    fun findAllByFilInfoId(filInfoId: Int): List<Transaksjon> =
        using(sessionOf(dataSource)) { session ->
            session.list(
                queryOf(
                    """
                    SELECT TRANSAKSJON_ID,
                    TRANS_TILSTAND_ID,
                    FIL_INFO_ID,
                    K_TRANSAKSJON_S,
                    PERSON_ID,
                    K_BELOP_T,
                    K_ART,
                    K_ANVISER,
                    FNR_FK,
                    UTBETALES_TIL,
                    OS_ID_FK,
                    OS_LINJE_ID_FK,
                    DATO_FOM,
                    DATO_TOM,
                    DATO_ANVISER,
                    DATO_PERSON_FOM,
                    DATO_REAK_FOM,
                    BELOP,
                    REF_TRANS_ID,
                    TEKSTKODE,
                    RECTYPE,
                    TRANS_EKS_ID_FK,
                    K_TRANS_TOLKNING,
                    SENDT_TIL_OPPDRAG,
                    TREKKVEDTAK_ID_FK,
                    FNR_ENDRET,
                    MOT_ID,
                    OS_STATUS,
                    DATO_OPPRETTET,
                    OPPRETTET_AV,
                    DATO_ENDRET,
                    ENDRET_AV,
                    VERSJON,
                    SALDO,
                    KID,
                    PRIORITET,
                    K_TREKKANSVAR,
                    K_TRANS_TILST_T,
                    GRAD
                            FROM T_TRANSAKSJON
                            WHERE FIL_INFO_ID = $filInfoId
                    """.trimIndent(),
                ),
                mapToTransaksjon,
            )
        }

    private val mapToAvregningstransaksjon: (Row) -> Avregningstransaksjon = { row ->
        Avregningstransaksjon(
            row.int("TRANSAKSJON_ID"),
            row.string("FNR_FK"),
            row.string("TRANS_EKS_ID_FK"),
            row.localDate("DATO_ANVISER"),
        )
    }

    private val mapToTransaksjon: (Row) -> Transaksjon = { row ->
        Transaksjon(
            transaksjonId = row.int("TRANSAKSJON_ID"),
            filInfoId = row.int("FIL_INFO_ID"),
            transaksjonStatus = row.string("K_TRANSAKSJON_S"),
            personId = row.int("PERSON_ID"),
            belopstype = row.string("K_BELOP_T"),
            art = row.string("K_ART"),
            anviser = row.string("K_ANVISER"),
            fnr = row.string("FNR_FK"),
            utbetalesTil = row.stringOrNull("UTBETALES_TIL"),
            osId = row.stringOrNull("OS_ID_FK"),
            osLinjeId = row.stringOrNull("OS_LINJE_ID_FK"),
            datoFom = row.localDateOrNull("DATO_FOM"),
            datoTom = row.localDateOrNull("DATO_TOM"),
            datoAnviser = row.localDateOrNull("DATO_ANVISER"),
            datoPersonFom = row.localDate("DATO_PERSON_FOM"),
            datoReakFom = row.localDateOrNull("DATO_REAK_FOM"),
            belop = row.int("BELOP"),
            refTransId = row.stringOrNull("REF_TRANS_ID"),
            tekstkode = row.stringOrNull("TEKSTKODE"),
            rectype = row.string("RECTYPE"),
            transEksId = row.string("TRANS_EKS_ID_FK"),
            transTolkning = row.string("K_TRANS_TOLKNING"),
            sendtTilOppdrag = row.string("SENDT_TIL_OPPDRAG"),
            trekkvedtakId = row.stringOrNull("TREKKVEDTAK_ID_FK"),
            fnrEndret = row.boolean("FNR_ENDRET").booleanToString(),
            motId = row.string("MOT_ID"),
            osStatus = row.stringOrNull("OS_STATUS"),
            datoOpprettet = row.localDateTime("DATO_OPPRETTET"),
            opprettetAv = row.string("OPPRETTET_AV"),
            datoEndret = row.localDateTime("DATO_ENDRET"),
            endretAv = row.string("ENDRET_AV"),
            versjon = row.int("VERSJON"),
            transTilstandType = row.stringOrNull("K_TRANS_TILST_T"),
            grad = row.intOrNull("GRAD"),
            trekkType = row.optionalOrNull("K_TREKK_T"),
            trekkGruppe = row.optionalOrNull("K_TREKKGRUPPE"),
            trekkAlternativ = row.optionalOrNull("K_TREKKALT_T"),
            gyldigKombinasjon =
                row.optionalOrNull<String>("K_FAGOMRADE")?.let {
                    GyldigKombinasjon(
                        fagomrade = row.string("K_FAGOMRADE"),
                        osKlassifikasjon = row.string("OS_KLASSIFIKASJON"),
                    )
                },
        )
    }
}
