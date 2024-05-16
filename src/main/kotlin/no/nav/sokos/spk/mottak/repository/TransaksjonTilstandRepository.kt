package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPR
import no.nav.sokos.spk.mottak.domain.TransaksjonTilstand

class TransaksjonTilstandRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    fun insertBatch(
        transaksjonIdList: List<Int>,
        session: Session,
    ): List<Int> {
        val systemId = PropertiesConfig.Configuration().naisAppName
        return session.batchPreparedNamedStatement(
            """
            INSERT INTO T_TRANS_TILSTAND (
                TRANSAKSJON_ID, 
                K_TRANS_TILST_T, 
                DATO_OPPRETTET, 
                OPPRETTET_AV, 
                DATO_ENDRET, 
                ENDRET_AV, 
                VERSJON
            ) VALUES (:transaksjonId, '$TRANS_TILSTAND_OPR', CURRENT_TIMESTAMP, '$systemId', CURRENT_TIMESTAMP, '$systemId', 1)
            """.trimIndent(),
            transaksjonIdList.map { mapOf("transaksjonId" to it) },
        )
    }

    fun getByTransaksjonId(transaksjonId: Int): TransaksjonTilstand? {
        return using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                    SELECT * FROM T_TRANS_TILSTAND WHERE TRANSAKSJON_ID = $transaksjonId;
                    """.trimIndent(),
                ),
                mapToTransaksjonTilstand,
            )
        }
    }

    private val mapToTransaksjonTilstand: (Row) -> TransaksjonTilstand = { row ->
        TransaksjonTilstand(
            row.int("TRANS_TILSTAND_ID"),
            row.int("TRANSAKSJON_ID"),
            row.string("K_TRANS_TILST_T"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON"),
        )
    }
}
