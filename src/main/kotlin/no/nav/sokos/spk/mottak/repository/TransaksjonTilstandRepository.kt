package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig

class TransaksjonTilstandRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource()
) {
    fun insert(transaksjonId: Int, session: Session): Long? {
        val systemId = PropertiesConfig.Configuration().naisAppName
        return session.run(
            queryOf(
                """
                    INSERT INTO T_TRANS_TILSTAND (
                        TRANSAKSJON_ID, 
                        K_TRANS_TILST_T, 
                        DATO_OPPRETTET, 
                        OPPRETTET_AV, 
                        DATO_ENDRET, 
                        ENDRET_AV, 
                        VERSJON
                    ) VALUES ($transaksjonId, 'OPR', CURRENT_TIMESTAMP, $systemId, CURRENT_TIMESTAMP, $systemId, 1)
                """.trimIndent()
            ).asUpdateAndReturnGeneratedKey
        )
    }
}