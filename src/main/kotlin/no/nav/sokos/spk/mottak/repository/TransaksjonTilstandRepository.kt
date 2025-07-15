package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.domain.TransaksjonTilstand
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics

class TransaksjonTilstandRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) {
    private val insertBatchTimer = Metrics.timer(DATABASE_CALL, "TransaksjonTilstandRepository", "insertBatch")
    private val getByTransaksjonIdTimer = Metrics.timer(DATABASE_CALL, "TransaksjonTilstandRepository", "getByTransaksjonId")
    private val findAllByTransaksjonIdTimer = Metrics.timer(DATABASE_CALL, "TransaksjonTilstandRepository", "findAllByTransaksjonId")

    fun insertBatch(
        transaksjonIdList: List<Int>,
        transaksjonTilstandType: String = TRANS_TILSTAND_OPPRETTET,
        systemId: String,
        feilkode: String? = null,
        feilkodeMelding: String? = null,
        session: Session,
    ): List<Int> =
        insertBatchTimer
            .recordCallable {
                session.batchPreparedNamedStatement(
                    """
                    INSERT INTO T_TRANS_TILSTAND (
                        TRANSAKSJON_ID,
                        K_TRANS_TILST_T, 
                        FEILKODE,
                        FEILKODEMELDING,
                        DATO_OPPRETTET, 
                        OPPRETTET_AV, 
                        DATO_ENDRET, 
                        ENDRET_AV, 
                        VERSJON
                    ) VALUES (:transaksjonId, '$transaksjonTilstandType', '$feilkode', '$feilkodeMelding', CURRENT_TIMESTAMP, '$systemId', CURRENT_TIMESTAMP, '$systemId', 1)
                    """.trimIndent(),
                    transaksjonIdList.map { mapOf("transaksjonId" to it) },
                )

                session.list(
                    queryOf(
                        """
                        SELECT MAX(TRANS_TILSTAND_ID) AS TRANS_TILSTAND_ID
                        FROM T_TRANS_TILSTAND
                        WHERE TRANSAKSJON_ID IN (${transaksjonIdList.joinToString()})
                          AND K_TRANS_TILST_T = '$transaksjonTilstandType'
                        GROUP BY TRANSAKSJON_ID
                        """.trimIndent(),
                    ),
                ) { row -> row.int("TRANS_TILSTAND_ID") }
            }.orEmpty()

    /** Bruker kun for testing */
    fun getByTransaksjonId(transaksjonId: Int): TransaksjonTilstand? =
        using(sessionOf(dataSource)) { session ->
            getByTransaksjonIdTimer.recordCallable {
                session.single(
                    queryOf(
                        """
                        SELECT TRANS_TILSTAND_ID, TRANSAKSJON_ID, K_TRANS_TILST_T, FEILKODE, FEILKODEMELDING, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON
                        FROM T_TRANS_TILSTAND 
                        WHERE TRANSAKSJON_ID = $transaksjonId;
                        """.trimIndent(),
                    ),
                    mapToTransaksjonTilstand,
                )
            }
        }

    fun findAllByTransaksjonId(transaksjonId: List<Int>): List<TransaksjonTilstand> =
        using(sessionOf(dataSource)) { session ->
            findAllByTransaksjonIdTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT TRANS_TILSTAND_ID, TRANSAKSJON_ID, K_TRANS_TILST_T, FEILKODE, FEILKODEMELDING, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON
                        FROM T_TRANS_TILSTAND 
                        WHERE TRANSAKSJON_ID IN (${transaksjonId.joinToString()});
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
            row.stringOrNull("FEILKODE"),
            row.stringOrNull("FEILKODEMELDING"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON"),
        )
    }
}
