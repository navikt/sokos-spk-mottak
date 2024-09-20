package no.nav.sokos.spk.mottak.repository

import kotliquery.Session
import kotliquery.queryOf

class OutboxRepository() {
    fun insertTrekk(
        meldinger: Map<Int, String>,
        session: Session,
    ) {
        session.batchPreparedNamedStatement(
            """
            INSERT INTO T_OUTBOX_TREKK (
                TRANSAKSJON_ID,
                MELDING
            ) VALUES (:transaksjonId, :melding)
            """.trimIndent(),
            meldinger.map { mapOf("transaksjonId" to it.key, "melding" to it.value) },
        )
    }

    fun insertUtbetaling(
        meldinger: Map<String, String>,
        session: Session,
    ) {
        session.batchPreparedNamedStatement(
            """
            INSERT INTO T_OUTBOX_UTBETALING (
                TRANSAKSJON_ID,
                MELDING
            ) VALUES (:transaksjonId, :melding)
            """.trimIndent(),
            meldinger.map { mapOf("transaksjonId" to it.key, "melding" to it.value) },
        )
    }

    fun getTrekk(session: Session): List<Pair<Int, String>> {
        return session.list(
            queryOf(
                """
                SELECT TRANSAKSJON_ID, MELDING 
                FROM T_OUTBOX_TREKK
                SKIP LOCKED DATA
                """.trimIndent(),
            ),
        ) { row -> Pair(row.int("TRANSAKSJON_ID"), row.string("MELDING")) }.orEmpty()
    }

    fun getUtbetaling(session: Session): List<Pair<String, String>> {
        return session.list(
            queryOf(
                """
                SELECT TRANSAKSJON_ID, MELDING 
                FROM T_OUTBOX_UTBETALING
                SKIP LOCKED DATA
                """.trimIndent(),
            ),
        ) { row -> Pair(row.string("TRANSAKSJON_ID"), row.string("MELDING")) }.orEmpty()
    }

    fun deleteTrekk(
        tansaksjonIdList: List<Int>,
        session: Session,
    ) {
        session.batchPreparedNamedStatement(
            """
            DELETE FROM T_OUTBOX_TREKK 
            WHERE TRANSAKSJON_ID = :transaksjonId
            SKIP LOCKED DATA
            """.trimIndent(),
            tansaksjonIdList.map { mapOf("transaksjonId" to it) },
        )
    }

    fun deleteUtbetaling(
        tansaksjonIdList: List<String>,
        session: Session,
    ) {
        session.batchPreparedNamedStatement(
            """
            DELETE FROM T_OUTBOX_UTBETALING 
            WHERE TRANSAKSJON_ID = :transaksjonId
            SKIP LOCKED DATA
            """.trimIndent(),
            tansaksjonIdList.map { mapOf("transaksjonId" to it) },
        )
    }
}
