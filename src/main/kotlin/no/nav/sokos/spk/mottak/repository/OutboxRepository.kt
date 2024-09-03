package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.config.DatabaseConfig

class OutboxRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    fun insert(
        meldinger: Map<Int, String>,
        session: Session,
    ) {
        session.batchPreparedNamedStatement(
            """
            INSERT INTO OUTBOX (
                TRANSAKSJON_ID,
                MELDING
            ) VALUES (:transaksjonId, :melding)
            """.trimIndent(),
            meldinger.map { mapOf("transaksjonId" to it.key, "melding" to it.value) },
        )
    }

    fun get(session: Session): Map<Int, String>? {
        return session.single(
            queryOf(
                """
                SELECT TRANSAKSJON_ID, MELDING 
                FROM OUTBOX 
                ORDER BY timestamp ASC
                SKIP LOCKED DATA
                """.trimIndent(),
            ),
        ) { row -> mapOf(row.int("TRANSAKSJON_ID") to row.string("MELDING")) }
    }

    fun delete(
        tansaksjonIdList: Set<Int>,
        session: Session,
    ) {
        session.update(
            queryOf(
                """
                DELETE FROM OUTBOX 
                WHERE TRANSAKSJON_ID = :transaksjonId
                SKIP LOCKED DATA
                """.trimIndent(),
                tansaksjonIdList.map { mapOf("transaksjonId" to it) },
            ),
        )
    }
}
