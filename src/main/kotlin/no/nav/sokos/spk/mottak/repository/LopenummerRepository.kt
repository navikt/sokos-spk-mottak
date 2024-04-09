package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.Lopenummer

class LopenummerRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource()
) {
    fun findMaxLopenummer(filType: String): Int? {
        return using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                        SELECT MAX(SISTE_LOPENR)
                        FROM  T_LOPENR
                        WHERE K_ANVISER = 'SPK'
                        AND K_FIL_T = (:filType)
                    """.trimIndent(),
                    mapOf("filType" to filType)
                )
            ) { row -> row.int(1) }
        }
    }

    fun getLopenummer(sisteLopenummer: Int): Lopenummer? {
        return sessionOf(dataSource).single(
            queryOf(
                """
                        SELECT * FROM T_LOPENR WHERE SISTE_LOPENR = :sisteLopenummer
                    """.trimIndent(),
                mapOf("sisteLopenummer" to sisteLopenummer)
            ), toLopenummer
        )
    }

    fun updateLopenummer(lopenummer: Int, filType: String, session: Session) {
        session.run(
            queryOf(
                """
                        UPDATE T_LOPENR 
                        SET SISTE_LOPENR = (:lopenummer), ENDRET_AV = '${PropertiesConfig.Configuration().naisAppName}', DATO_ENDRET = CURRENT_TIMESTAMP 
                        WHERE K_ANVISER = 'SPK'
                        AND K_FIL_T = (:filType)
                    """.trimIndent(),
                mapOf(
                    "lopenummer" to lopenummer,
                    "filType" to filType
                )
            ).asUpdate
        )
    }


    private val toLopenummer: (Row) -> Lopenummer = { row ->
        Lopenummer(
            row.int("LOPENR_ID"),
            row.int("SISTE_LOPENR"),
            row.string("K_FIL_T"),
            row.string("K_ANVISER"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON")
        )
    }
}
