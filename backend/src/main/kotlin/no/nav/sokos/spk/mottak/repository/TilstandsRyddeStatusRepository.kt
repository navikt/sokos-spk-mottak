package no.nav.sokos.spk.mottak.repository

import java.sql.Connection
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDateTime

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Parameter
import kotliquery.Session
import kotliquery.queryOf

import kotliquery.Connection as KotliqueryConnection
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics

const val TILSTANDSRYDDING_LASENOKKEL = 0x53504B5259444445L

data class TilstandsRyddeStatus(
    val regelversjon: String,
    val oppbevaringMaaneder: Int,
    val grensetidspunkt: LocalDateTime,
    val forsteId: Long?,
    val ovreId: Long?,
    val sisteFerdigeId: Long?,
    val fullfort: Boolean,
    val sisteFullfortTidspunkt: Instant?,
) {
    init {
        require(regelversjon.isNotBlank() && regelversjon.length <= 100)
        require(oppbevaringMaaneder >= 6)
        require(grensetidspunkt.nano % 1000 == 0)
        require(
            if (forsteId == null || ovreId == null) {
                forsteId == null && ovreId == null && sisteFerdigeId == null && fullfort
            } else {
                forsteId in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong() &&
                    ovreId in forsteId..Int.MAX_VALUE.toLong() &&
                    (sisteFerdigeId == null || sisteFerdigeId in forsteId..ovreId)
            },
        )
    }
}

interface Kjoringslas : AutoCloseable {
    fun kontroller(timeoutSekunder: Int = 5)
}

class RyddeLasTapt : IllegalStateException("Kjøringslåsen er ikke lenger eid av sesjonen")

class TilstandsRyddeStatusRepository(
    // Hikari kreves for å forkaste fysiske låsesesjoner og kontrollere at poolen har kapasitet utover låseforbindelsen.
    private val dataSource: HikariDataSource,
) {
    init {
        require(dataSource.maximumPoolSize >= 2) { "Kjøringslåsen trenger en egen forbindelse og ledig poolkapasitet" }
    }

    fun provKjoringslas(kontroll: RyddeEnhetskontroll = RyddeEnhetskontroll { 5 }): Kjoringslas? =
        malt("provKjoringslas") {
            kontroll.timeoutSekunder()
            val forbindelse = dataSource.connection
            try {
                val tatt =
                    sesjon(forbindelse, kontroll.timeoutSekunder()).single(
                        queryOf("SELECT pg_try_advisory_lock(?)", TILSTANDSRYDDING_LASENOKKEL),
                    ) { rad -> rad.boolean(1) }!!
                kontroll.timeoutSekunder()
                if (tatt) Sesjonslas(forbindelse, kontroll) else null.also { forbindelse.close() }
            } catch (feil: Exception) {
                forkast(forbindelse)
                throw feil
            }
        }

    fun hent(
        las: Kjoringslas,
        kontroll: RyddeEnhetskontroll = RyddeEnhetskontroll { 5 },
    ): TilstandsRyddeStatus? =
        malt("hent") {
            las.kontroller(kontroll.timeoutSekunder())
            kontroll.timeoutSekunder()
            val status =
                dataSource.connection.use { forbindelse ->
                    kontroll.timeoutSekunder()
                    sesjon(forbindelse, kontroll.timeoutSekunder()).single(
                        queryOf("SELECT * FROM tilstandsrydding_status WHERE id = 1"),
                    ) { rad ->
                        TilstandsRyddeStatus(
                            rad.string("regelversjon"),
                            rad.int("oppbevaring_maaneder"),
                            rad.localDateTime("grensetidspunkt"),
                            rad.longOrNull("forste_id"),
                            rad.longOrNull("ovre_id"),
                            rad.longOrNull("siste_ferdige_id"),
                            rad.boolean("fullfort"),
                            rad.sqlTimestampOrNull("siste_fullfort_tidspunkt")?.toInstant(),
                        )
                    }
                }
            kontroll.timeoutSekunder()
            status
        }

    fun lagre(
        las: Kjoringslas,
        status: TilstandsRyddeStatus,
        kontroll: RyddeEnhetskontroll = RyddeEnhetskontroll { 5 },
    ) {
        malt("lagre") {
            las.kontroller(kontroll.timeoutSekunder())
            kontroll.timeoutSekunder()
            dataSource.connection.use { forbindelse ->
                kontroll.timeoutSekunder()
                forbindelse.autoCommit = false
                try {
                    sesjon(forbindelse, kontroll.timeoutSekunder()).update(
                        queryOf(
                            """
                            INSERT INTO tilstandsrydding_status
                                (id, regelversjon, oppbevaring_maaneder, grensetidspunkt, forste_id, ovre_id,
                                 siste_ferdige_id, fullfort, siste_fullfort_tidspunkt)
                            VALUES (1, ?, ?, ?, ?, ?, ?, ?, ?)
                            ON CONFLICT (id) DO UPDATE SET
                                regelversjon = EXCLUDED.regelversjon,
                                oppbevaring_maaneder = EXCLUDED.oppbevaring_maaneder,
                                grensetidspunkt = EXCLUDED.grensetidspunkt,
                                forste_id = EXCLUDED.forste_id,
                                ovre_id = EXCLUDED.ovre_id,
                                siste_ferdige_id = EXCLUDED.siste_ferdige_id,
                                fullfort = EXCLUDED.fullfort,
                                sist_oppdatert = CURRENT_TIMESTAMP,
                                siste_fullfort_tidspunkt = COALESCE(EXCLUDED.siste_fullfort_tidspunkt,
                                    tilstandsrydding_status.siste_fullfort_tidspunkt)
                            """.trimIndent(),
                            status.regelversjon,
                            status.oppbevaringMaaneder,
                            Timestamp.valueOf(status.grensetidspunkt),
                            Parameter(status.forsteId, Long::class.javaPrimitiveType!!),
                            Parameter(status.ovreId, Long::class.javaPrimitiveType!!),
                            Parameter(status.sisteFerdigeId, Long::class.javaPrimitiveType!!),
                            status.fullfort,
                            Parameter(status.sisteFullfortTidspunkt?.let(Timestamp::from), Timestamp::class.java),
                        ),
                    )
                    las.kontroller(kontroll.timeoutSekunder())
                    kontroll.timeoutSekunder()
                    forbindelse.commit()
                } catch (feil: Exception) {
                    try {
                        forbindelse.rollback()
                    } catch (rollbackFeil: Exception) {
                        feil.addSuppressed(rollbackFeil)
                    }
                    throw feil
                }
            }
        }
    }

    private inner class Sesjonslas(
        private val forbindelse: Connection,
        private val kontroll: RyddeEnhetskontroll,
    ) : Kjoringslas {
        private var lukket = false

        override fun kontroller(timeoutSekunder: Int) {
            check(!lukket) { "Kjøringslåsen er lukket" }
            val timeout = minOf(timeoutSekunder, kontroll.timeoutSekunder())
            val eid =
                malt("kontrollerLas") {
                    require(timeoutSekunder > 0)
                    sesjon(forbindelse, minOf(timeout, kontroll.timeoutSekunder())).single(
                        queryOf(
                            """
                            SELECT EXISTS (
                                SELECT 1 FROM pg_locks
                                WHERE locktype = 'advisory' AND pid = pg_backend_pid() AND granted
                                  AND classid = ?::oid AND objid = ?::oid AND objsubid = 1
                            )
                            """.trimIndent(),
                            TILSTANDSRYDDING_LASENOKKEL ushr 32,
                            TILSTANDSRYDDING_LASENOKKEL and 0xffffffffL,
                        ),
                    ) { rad -> rad.boolean(1) }!!
                }
            kontroll.timeoutSekunder()
            if (!eid) throw RyddeLasTapt()
        }

        override fun close() {
            if (lukket) return
            lukket = true
            val timeout =
                try {
                    kontroll.timeoutSekunder()
                } catch (_: no.nav.sokos.spk.mottak.domain.RyddeStopp) {
                    forkast(forbindelse)
                    return
                }
            try {
                malt("frigjorLas") {
                    val frigjort =
                        sesjon(forbindelse, timeout).single(
                            queryOf("SELECT pg_advisory_unlock(?)", TILSTANDSRYDDING_LASENOKKEL),
                        ) { rad -> rad.boolean(1) }!!
                    if (!frigjort) throw RyddeLasTapt()
                }
            } catch (feil: Exception) {
                forkast(forbindelse)
                throw feil
            } finally {
                forbindelse.close()
            }
        }
    }

    private fun forkast(forbindelse: Connection) {
        // Ved ukjent låseutfall eller manglende unlock kan sesjonen fortsatt holde en PostgreSQL advisory lock.
        // close() alene returnerer forbindelsen til poolen; eviction avslutter den fysiske sesjonen og frigjør sesjonslåsen.
        try {
            dataSource.evictConnection(forbindelse)
        } finally {
            // Eviction lukker den fysiske sesjonen før proxyen kan returneres til poolen.
            forbindelse.close()
        }
    }

    private fun sesjon(
        forbindelse: Connection,
        timeoutSekunder: Int,
    ): Session {
        require(timeoutSekunder > 0)
        // Sesjonens close lukker også forbindelsen, som eies av kalleren.
        return Session(
            KotliqueryConnection(forbindelse),
            returnGeneratedKeys = false,
            queryTimeout = timeoutSekunder,
        )
    }

    private fun <T> malt(
        metode: String,
        handling: () -> T,
    ): T = Metrics.timer(DATABASE_CALL, "TilstandsRyddeStatusRepository", metode).recordCallable { Pair(handling(), Unit) }!!.first
}
