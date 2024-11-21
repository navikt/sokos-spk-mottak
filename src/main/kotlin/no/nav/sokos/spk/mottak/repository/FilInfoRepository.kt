package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.AvstemmingInfo
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_OK
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.util.SQLUtils.asMap

class FilInfoRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    private val getByFilTilstandAndAllInnTransaksjonIsBehandletTimer = Metrics.timer(DATABASE_CALL, "FilInfoRepository", "getByFilTilstandAndAllInnTransaksjonIsBehandlet")
    private val getByAvstemmingStatusIsOSOTimer = Metrics.timer(DATABASE_CALL, "FilInfoRepository", "getByAvstemmingStatusIsOSO")
    private val insertTimer = Metrics.timer(DATABASE_CALL, "FilInfoRepository", "insert")

    fun getByFilTilstandAndAllInnTransaksjonIsBehandlet(filTilstandType: String = FILTILSTANDTYPE_GOD): List<FilInfo> =
        using(sessionOf(dataSource)) { session ->
            getByFilTilstandAndAllInnTransaksjonIsBehandletTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT FIL_INFO_ID, K_FIL_S, K_ANVISER, K_FIL_T, K_FIL_TILSTAND_T, FIL_NAVN, LOPENR, FEILTEKST, DATO_MOTTATT, DATO_SENDT, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, K_AVSTEMMING_S
                        FROM T_FIL_INFO
                        WHERE K_FIL_TILSTAND_T = :filTilstandType
                        AND K_ANVISER = '$SPK'
                        AND K_FIL_S = '${FilStatus.OK.code}'
                        AND FIL_INFO_ID IN (select FIL_INFO_ID
                        FROM T_INN_TRANSAKSJON
                        GROUP BY FIL_INFO_ID
                        HAVING SUM(CASE WHEN BEHANDLET = 'J' THEN 0 ELSE 1 END) = 0)
                        """.trimIndent(),
                        mapOf("filTilstandType" to filTilstandType),
                    ),
                    mapToFileInfo,
                )
            }
        }

    fun getByAvstemmingStatusIsOSO(
        antallUkjentOSZStatus: Int,
        statusFilter: Boolean = true,
    ): List<AvstemmingInfo> =
        using(sessionOf(dataSource)) { session ->
            getByAvstemmingStatusIsOSOTimer.recordCallable {
                session
                    .list(
                        queryOf(
                            """
                            SELECT
                                t.FIL_INFO_ID,
                                COUNT(CASE WHEN t.OS_STATUS IS NOT NULL THEN 1 END) AS ANTALL,
                                COUNT(CASE WHEN t.OS_STATUS IS NULL THEN 1 END) AS ANTALL_NULL    
                            from T_FIL_INFO fi INNER JOIN T_TRANSAKSJON t ON fi.FIL_INFO_ID = t.FIL_INFO_ID
                            where fi.K_ANVISER = '$SPK' AND fi.K_AVSTEMMING_S = '$TRANS_TILSTAND_OPPDRAG_SENDT_OK' AND t.K_BELOP_T IN ('01', '02') 
                            group by t.FIL_INFO_ID
                            ${if (statusFilter) " having COUNT(CASE WHEN t.OS_STATUS IS NULL THEN 1 END) <= $antallUkjentOSZStatus" else ""}
                            """.trimIndent(),
                        ),
                    ) { row ->
                        AvstemmingInfo(
                            filInfoId = row.int("FIL_INFO_ID"),
                            antallOSStatus = row.int("ANTALL"),
                            antallIkkeOSStatus = row.int("ANTALL_NULL"),
                        )
                    }
            }
        }

    fun insert(
        filInfo: FilInfo,
        session: Session,
    ): Long? =
        insertTimer.recordCallable {
            session.updateAndReturnGeneratedKey(
                queryOf(
                    """
                    INSERT INTO T_FIL_INFO (
                    K_FIL_S,
                    K_FIL_TILSTAND_T,
                    K_ANVISER,
                    FIL_NAVN,
                    LOPENR,
                    DATO_MOTTATT,
                    DATO_OPPRETTET,
                    OPPRETTET_AV,
                    DATO_ENDRET,
                    ENDRET_AV,
                    VERSJON,
                    K_FIL_T,
                    FEILTEKST ) VALUES (:filStatus, :filTilstandType, :anviser, :filNavn, :lopeNr, :datoMottatt, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :filType, :feilTekst)
                    """.trimIndent(),
                    filInfo.asMap(),
                ),
            )
        }

    fun updateAvstemmingStatus(
        filInfoIdList: List<Int?>,
        avstemmingStatus: String,
        session: Session,
    ) {
        session.update(
            queryOf(
                """
                UPDATE T_FIL_INFO SET K_AVSTEMMING_S = '$avstemmingStatus', DATO_ENDRET = CURRENT_TIMESTAMP, ENDRET_AV = '${PropertiesConfig.Configuration().naisAppName}'
                WHERE FIL_INFO_ID IN (${filInfoIdList.joinToString()})
                """.trimIndent(),
            ),
        )
    }

    /**
     * Bruker kun for testing
     */
    fun getByLopenummerAndFilTilstand(
        lopeNummer: String,
        filTilstandType: String,
    ): FilInfo? =
        using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                    SELECT FIL_INFO_ID, K_FIL_S, K_ANVISER, K_FIL_T, K_FIL_TILSTAND_T, FIL_NAVN, LOPENR, FEILTEKST, DATO_MOTTATT, DATO_SENDT, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, K_AVSTEMMING_S
                    FROM T_FIL_INFO 
                    WHERE LOPENR = :lopeNummer AND K_FIL_TILSTAND_T = :filTilstandType AND K_ANVISER = '$SPK'
                    """.trimIndent(),
                    mapOf(
                        "lopeNummer" to lopeNummer,
                        "filTilstandType" to filTilstandType,
                    ),
                ),
                mapToFileInfo,
            )
        }

    private val mapToFileInfo: (Row) -> FilInfo = { row ->
        FilInfo(
            row.int("FIL_INFO_ID"),
            row.string("K_FIL_S"),
            row.string("K_ANVISER"),
            row.string("K_FIL_T"),
            row.string("K_FIL_TILSTAND_T"),
            row.string("FIL_NAVN"),
            row.string("LOPENR"),
            row.stringOrNull("FEILTEKST"),
            row.localDateOrNull("DATO_MOTTATT"),
            row.localDateOrNull("DATO_SENDT"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON"),
            row.stringOrNull("K_AVSTEMMING_S"),
        )
    }
}
