package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.LopeNummer
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics

class LopenummerRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) {
    private val findMaxLopeNummerTimer = Metrics.timer(DATABASE_CALL, "LopenummerRepository", "findMaxLopeNummer")
    private val updateLopeNummerTimer = Metrics.timer(DATABASE_CALL, "LopenummerRepository", "updateLopeNummer")

    fun findMaxLopeNummer(filType: String): Int? =
        using(sessionOf(dataSource)) { session ->
            findMaxLopeNummerTimer.recordCallable {
                session.single(
                    queryOf(
                        """
                        SELECT MAX(SISTE_LOPENR)
                        FROM  T_LOPENR
                        WHERE K_ANVISER = 'SPK'
                        AND K_FIL_T = (:filType)
                        """.trimIndent(),
                        mapOf("filType" to filType),
                    ),
                ) { row -> row.int(1) }
            }
        }

    fun updateLopeNummer(
        lopeNummer: String,
        filType: String,
        session: Session,
    ) {
        updateLopeNummerTimer.recordCallable {
            session.update(
                queryOf(
                    """
                    UPDATE T_LOPENR 
                    SET SISTE_LOPENR = (:lopeNummer), ENDRET_AV = '${PropertiesConfig.Configuration().naisAppName}', DATO_ENDRET = CURRENT_TIMESTAMP 
                    WHERE K_ANVISER = 'SPK'
                    AND K_FIL_T = (:filType)
                    """.trimIndent(),
                    mapOf(
                        "lopeNummer" to lopeNummer,
                        "filType" to filType,
                    ),
                ),
            )
        }
    }

    /**
     * Bruker kun for testing
     */
    fun getLopeNummer(sisteLopeNummer: String): LopeNummer? =
        using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                    SELECT LOPENR_ID, SISTE_LOPENR, K_FIL_T, K_ANVISER, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON
                    FROM T_LOPENR 
                    WHERE SISTE_LOPENR = :sisteLopeNummer
                    """.trimIndent(),
                    mapOf("sisteLopeNummer" to sisteLopeNummer),
                ),
                mapToLopeNummer,
            )
        }

    private val mapToLopeNummer: (Row) -> LopeNummer = { row ->
        LopeNummer(
            row.int("LOPENR_ID"),
            row.int("SISTE_LOPENR"),
            row.string("K_FIL_T"),
            row.string("K_ANVISER"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON"),
        )
    }
}
