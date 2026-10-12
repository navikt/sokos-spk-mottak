package no.nav.sokos.spk.mottak.repository

import java.sql.Connection
import java.sql.Timestamp
import java.sql.Types
import java.time.Instant
import java.time.LocalDateTime

import com.zaxxer.hikari.HikariDataSource

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

interface TilstandsRyddeStatuslager {
    fun provKjoringslas(kontroll: RyddeEnhetskontroll = RyddeEnhetskontroll { 5 }): Kjoringslas?

    fun hent(
        las: Kjoringslas,
        kontroll: RyddeEnhetskontroll = RyddeEnhetskontroll { 5 },
    ): TilstandsRyddeStatus?

    fun lagre(
        las: Kjoringslas,
        status: TilstandsRyddeStatus,
        kontroll: RyddeEnhetskontroll = RyddeEnhetskontroll { 5 },
    )
}

class TilstandsRyddeStatusRepository(
    private val dataSource: HikariDataSource,
) : TilstandsRyddeStatuslager {
    init {
        require(dataSource.maximumPoolSize >= 2) { "Kjøringslåsen trenger en egen forbindelse og ledig poolkapasitet" }
    }

    override fun provKjoringslas(kontroll: RyddeEnhetskontroll): Kjoringslas? =
        malt("provKjoringslas") {
            kontroll.timeoutSekunder()
            val forbindelse = dataSource.connection
            try {
                val tatt =
                    forbindelse.prepareStatement("SELECT pg_try_advisory_lock(?)").use {
                        it.queryTimeout = kontroll.timeoutSekunder()
                        it.setLong(1, TILSTANDSRYDDING_LASENOKKEL)
                        it.executeQuery().use { resultat ->
                            resultat.next()
                            resultat.getBoolean(1)
                        }
                    }
                kontroll.timeoutSekunder()
                if (tatt) Sesjonslas(forbindelse, kontroll) else null.also { forbindelse.close() }
            } catch (feil: Exception) {
                forkast(forbindelse)
                throw feil
            }
        }

    override fun hent(
        las: Kjoringslas,
        kontroll: RyddeEnhetskontroll,
    ): TilstandsRyddeStatus? =
        malt("hent") {
            las.kontroller(kontroll.timeoutSekunder())
            kontroll.timeoutSekunder()
            val status =
                dataSource.connection.use { forbindelse ->
                    kontroll.timeoutSekunder()
                    forbindelse.prepareStatement("SELECT * FROM tilstandsrydding_status WHERE id = 1").use { setning ->
                        setning.queryTimeout = kontroll.timeoutSekunder()
                        setning.executeQuery().use { resultat ->
                            if (!resultat.next()) {
                                null
                            } else {
                                TilstandsRyddeStatus(
                                    resultat.getString("regelversjon"),
                                    resultat.getInt("oppbevaring_maaneder"),
                                    resultat.getTimestamp("grensetidspunkt").toLocalDateTime(),
                                    (resultat.getObject("forste_id") as Number?)?.toLong(),
                                    (resultat.getObject("ovre_id") as Number?)?.toLong(),
                                    (resultat.getObject("siste_ferdige_id") as Number?)?.toLong(),
                                    resultat.getBoolean("fullfort"),
                                    resultat.getTimestamp("siste_fullfort_tidspunkt")?.toInstant(),
                                )
                            }
                        }
                    }
                }
            kontroll.timeoutSekunder()
            status
        }

    override fun lagre(
        las: Kjoringslas,
        status: TilstandsRyddeStatus,
        kontroll: RyddeEnhetskontroll,
    ) {
        malt("lagre") {
            las.kontroller(kontroll.timeoutSekunder())
            kontroll.timeoutSekunder()
            dataSource.connection.use { forbindelse ->
                kontroll.timeoutSekunder()
                forbindelse.autoCommit = false
                try {
                    forbindelse
                        .prepareStatement(
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
                        ).use { setning ->
                            setning.queryTimeout = kontroll.timeoutSekunder()
                            setning.setString(1, status.regelversjon)
                            setning.setInt(2, status.oppbevaringMaaneder)
                            setning.setTimestamp(3, Timestamp.valueOf(status.grensetidspunkt))
                            listOf(status.forsteId, status.ovreId, status.sisteFerdigeId).forEachIndexed { indeks, id ->
                                if (id == null) setning.setNull(indeks + 4, Types.BIGINT) else setning.setLong(indeks + 4, id)
                            }
                            setning.setBoolean(7, status.fullfort)
                            if (status.sisteFullfortTidspunkt == null) {
                                setning.setNull(8, Types.TIMESTAMP_WITH_TIMEZONE)
                            } else {
                                setning.setTimestamp(8, Timestamp.from(status.sisteFullfortTidspunkt))
                            }
                            setning.executeUpdate()
                        }
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
                    forbindelse
                        .prepareStatement(
                            """
                            SELECT EXISTS (
                                SELECT 1 FROM pg_locks
                                WHERE locktype = 'advisory' AND pid = pg_backend_pid() AND granted
                                  AND classid = ?::oid AND objid = ?::oid AND objsubid = 1
                            )
                            """.trimIndent(),
                        ).use { setning ->
                            require(timeoutSekunder > 0)
                            setning.queryTimeout = minOf(timeout, kontroll.timeoutSekunder())
                            setning.setLong(1, TILSTANDSRYDDING_LASENOKKEL ushr 32)
                            setning.setLong(2, TILSTANDSRYDDING_LASENOKKEL and 0xffffffffL)
                            setning.executeQuery().use { resultat ->
                                resultat.next()
                                resultat.getBoolean(1)
                            }
                        }
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
                    forbindelse.prepareStatement("SELECT pg_advisory_unlock(?)").use {
                        it.queryTimeout = timeout
                        it.setLong(1, TILSTANDSRYDDING_LASENOKKEL)
                        val frigjort =
                            it.executeQuery().use { resultat ->
                                resultat.next()
                                resultat.getBoolean(1)
                            }
                        if (!frigjort) throw RyddeLasTapt()
                    }
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
        try {
            dataSource.evictConnection(forbindelse)
        } finally {
            // Eviction lukker den fysiske sesjonen før proxyen kan returneres til poolen.
            forbindelse.close()
        }
    }

    private fun <T> malt(
        metode: String,
        handling: () -> T,
    ): T = Metrics.timer(DATABASE_CALL, "TilstandsRyddeStatusRepository", metode).recordCallable { Pair(handling(), Unit) }!!.first
}
