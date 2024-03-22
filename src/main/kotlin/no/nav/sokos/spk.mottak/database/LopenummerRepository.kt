package no.nav.sokos.spk.mottak.database

import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.database.config.HikariConfig
import javax.sql.DataSource

class LopenummerRepository(
    private val dataSource: DataSource = HikariConfig.hikariDataSource()
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

    fun updateLopenummer(lopenummer: Int, filType: String, session: Session) {
        session.run(
            queryOf(
                """
                        UPDATE T_LOPENR 
                        SET SISTE_LOPENR = (:lopenummer)
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
}
