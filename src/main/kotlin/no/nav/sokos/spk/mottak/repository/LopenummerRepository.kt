package no.nav.sokos.spk.mottak.repository

import javax.sql.DataSource
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig

class LopenummerRepository(
    private val dataSource: DataSource = DatabaseConfig.hikariDataSource()
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
}
