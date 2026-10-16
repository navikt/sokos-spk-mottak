package no.nav.sokos.spk.mottak.repository

import java.sql.Timestamp
import java.time.LocalDateTime

import kotliquery.TransactionalSession
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.domain.TilstandsRyddeRegel
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics

data class Db2RyddeSnapshot(
    val start: LocalDateTime,
    val minId: Int?,
    val maxId: Int?,
)

data class RyddeBatchresultat(
    val slettede: Int,
    val nullstilteReferanser: Int,
)

class ReferansegrenseOverskredet : IllegalStateException("Referansegrensen for batchen er overskredet")

class RyddeSletteavvik : IllegalStateException("Sletteantallet avviker fra bekreftede og låste tilstander")

class RyddeUavklartUtfall(
    arsak: Throwable,
) : IllegalStateException("Transaksjonsutfallet er ikke bekreftet", arsak)

fun interface RyddeEnhetskontroll {
    fun timeoutSekunder(): Int
}

enum class RyddeSqlDialekt(
    val tidspunkt: String,
    val lasing: String,
) {
    DB2("CURRENT TIMESTAMP", "FOR UPDATE WITH RS USE AND KEEP UPDATE LOCKS"),
    H2("LOCALTIMESTAMP(6)", "FOR UPDATE"),
}

class TilstandsRyddeRepository(
    private val dialekt: RyddeSqlDialekt = RyddeSqlDialekt.DB2,
) {
    fun hentSnapshot(
        session: TransactionalSession,
        kontroll: RyddeEnhetskontroll,
    ): Db2RyddeSnapshot =
        malt("hentSnapshot") {
            kontroll.timeoutSekunder()
            session
                .single(
                    queryOf(
                        "SELECT ${dialekt.tidspunkt} AS RUNDESTART, MIN(TRANS_TILSTAND_ID) AS MIN_ID, MAX(TRANS_TILSTAND_ID) AS MAX_ID FROM T_TRANS_TILSTAND",
                    ),
                ) { rad ->
                    Db2RyddeSnapshot(
                        rad.sqlTimestamp("RUNDESTART").toLocalDateTime(),
                        rad.intOrNull("MIN_ID"),
                        rad.intOrNull("MAX_ID"),
                    )
                }.also { kontroll.timeoutSekunder() }!!
        }

    fun finnKandidater(
        session: TransactionalSession,
        fraId: Long,
        tilId: Long,
        regel: TilstandsRyddeRegel,
        grense: LocalDateTime,
        kontroll: RyddeEnhetskontroll,
    ): List<Int> =
        malt("finnKandidater") {
            require(fraId in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
            require(tilId in fraId..Int.MAX_VALUE.toLong() && tilId - fraId < 10000)
            kontroll.timeoutSekunder()
            session
                .list(
                    queryOf(
                        """
                        SELECT TRANS_TILSTAND_ID FROM T_TRANS_TILSTAND
                        WHERE TRANS_TILSTAND_ID BETWEEN ? AND ? AND ${regel.alderspredikat}
                        ORDER BY TRANS_TILSTAND_ID
                        """.trimIndent(),
                        fraId.toInt(),
                        tilId.toInt(),
                        Timestamp.valueOf(grense),
                    ),
                ) { rad -> rad.int(1) }
                .also { kontroll.timeoutSekunder() }
        }

    fun slettUrefererte(
        session: TransactionalSession,
        ider: List<Int>,
        regel: TilstandsRyddeRegel,
        grense: LocalDateTime,
        kontroll: RyddeEnhetskontroll,
    ): RyddeBatchresultat {
        kontrollerBatch(ider)
        if (ider.isEmpty()) return RyddeBatchresultat(0, 0)
        return malt("slettUrefererte") {
            kontroll.timeoutSekunder()
            val slettede =
                session.update(
                    queryOf(
                        """
                        DELETE FROM T_TRANS_TILSTAND s
                        WHERE s.TRANS_TILSTAND_ID IN (${plassholdere(ider)}) AND ${regel.alderspredikat}
                          AND NOT EXISTS (SELECT 1 FROM T_TRANSAKSJON t WHERE t.TRANS_TILSTAND_ID = s.TRANS_TILSTAND_ID)
                        """.trimIndent(),
                        *ider.toTypedArray(),
                        Timestamp.valueOf(grense),
                    ),
                )
            kontroll.timeoutSekunder()
            RyddeBatchresultat(slettede, 0)
        }
    }

    fun nullstillOgSlett(
        session: TransactionalSession,
        ider: List<Int>,
        regel: TilstandsRyddeRegel,
        grense: LocalDateTime,
        maksReferanser: Int,
        kontroll: RyddeEnhetskontroll,
    ): RyddeBatchresultat {
        kontrollerBatch(ider)
        require(maksReferanser in 1 until Int.MAX_VALUE)
        if (ider.isEmpty()) return RyddeBatchresultat(0, 0)
        return malt("nullstillOgSlett") {
            kontroll.timeoutSekunder()
            // Db2 z/OS-syntaks og låsevirkning må godkjennes i DBA-testen; H2 beviser bare atomisiteten.
            val bekreftede =
                session
                    .list(
                        queryOf(
                            """
                            SELECT TRANS_TILSTAND_ID FROM T_TRANS_TILSTAND
                            WHERE TRANS_TILSTAND_ID IN (${plassholdere(ider)}) AND ${regel.alderspredikat}
                            ${dialekt.lasing}
                            """.trimIndent(),
                            *ider.toTypedArray(),
                            Timestamp.valueOf(grense),
                        ),
                    ) { rad -> rad.int(1) }
            kontroll.timeoutSekunder()
            if (bekreftede.isEmpty()) return@malt RyddeBatchresultat(0, 0)

            val referanseantall =
                session
                    .list(
                        queryOf(
                            """
                            SELECT TRANSAKSJON_ID FROM T_TRANSAKSJON
                            WHERE TRANS_TILSTAND_ID IN (${plassholdere(bekreftede)})
                            FETCH FIRST ${maksReferanser + 1} ROWS ONLY
                            """.trimIndent(),
                            *bekreftede.toTypedArray(),
                        ),
                    ) { 1 }
                    .size
            kontroll.timeoutSekunder()
            if (referanseantall > maksReferanser) throw ReferansegrenseOverskredet()

            val nullstilte =
                session.update(
                    queryOf(
                        "UPDATE T_TRANSAKSJON SET TRANS_TILSTAND_ID = NULL WHERE TRANS_TILSTAND_ID IN (${plassholdere(bekreftede)})",
                        *bekreftede.toTypedArray(),
                    ),
                )
            kontroll.timeoutSekunder()
            if (nullstilte > maksReferanser) throw ReferansegrenseOverskredet()

            val slettede =
                session.update(
                    queryOf(
                        "DELETE FROM T_TRANS_TILSTAND WHERE TRANS_TILSTAND_ID IN (${plassholdere(bekreftede)}) AND ${regel.alderspredikat}",
                        *bekreftede.toTypedArray(),
                        Timestamp.valueOf(grense),
                    ),
                )
            kontroll.timeoutSekunder()
            if (slettede != bekreftede.size) throw RyddeSletteavvik()
            RyddeBatchresultat(slettede, nullstilte)
        }
    }

    private fun <T> malt(
        metode: String,
        handling: () -> T,
    ): T = Metrics.timer(DATABASE_CALL, "TilstandsRyddeRepository", metode).recordCallable(handling)!!

    private fun kontrollerBatch(ider: List<Int>) {
        require(ider.size <= 100 && ider.distinct().size == ider.size)
    }

    private fun plassholdere(ider: List<Int>): String = ider.joinToString { "?" }
}
