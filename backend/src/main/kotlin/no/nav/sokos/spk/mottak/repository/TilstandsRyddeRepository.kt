package no.nav.sokos.spk.mottak.repository

import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Timestamp
import java.time.LocalDateTime
import javax.sql.DataSource

import no.nav.sokos.spk.mottak.domain.TilstandsRyddeRegel
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics

data class Db2RyddeSnapshot(val start: LocalDateTime, val minId: Int?, val maxId: Int?)

data class RyddeBatchresultat(val slettede: Int, val nullstilteReferanser: Int)

class ReferansegrenseOverskredet : IllegalStateException("Referansegrensen for batchen er overskredet")

class RyddeSletteavvik : IllegalStateException("Sletteantallet avviker fra bekreftede og låste tilstander")

class RyddeUavklartUtfall(arsak: Throwable) : IllegalStateException("Transaksjonsutfallet er ikke bekreftet", arsak)

fun interface RyddeEnhetskontroll {
    fun timeoutSekunder(): Int
}

enum class RyddeSqlDialekt(val tidspunkt: String, val lasing: String) {
    DB2("CURRENT TIMESTAMP", "FOR UPDATE WITH RS USE AND KEEP UPDATE LOCKS"),
    H2("LOCALTIMESTAMP(6)", "FOR UPDATE"),
}

interface TilstandsRyddeDatakilde {
    fun hentSnapshot(kontroll: RyddeEnhetskontroll): Db2RyddeSnapshot

    fun finnKandidater(
        fraId: Long,
        tilId: Long,
        regel: TilstandsRyddeRegel,
        grense: LocalDateTime,
        kontroll: RyddeEnhetskontroll,
    ): List<Int>

    fun slettUrefererte(
        ider: List<Int>,
        regel: TilstandsRyddeRegel,
        grense: LocalDateTime,
        kontroll: RyddeEnhetskontroll,
    ): RyddeBatchresultat

    fun nullstillOgSlett(
        ider: List<Int>,
        regel: TilstandsRyddeRegel,
        grense: LocalDateTime,
        maksReferanser: Int,
        kontroll: RyddeEnhetskontroll,
    ): RyddeBatchresultat
}

class TilstandsRyddeRepository(
    private val dataSource: DataSource,
    private val dialekt: RyddeSqlDialekt = RyddeSqlDialekt.DB2,
) : TilstandsRyddeDatakilde {
    override fun hentSnapshot(kontroll: RyddeEnhetskontroll): Db2RyddeSnapshot =
        transaksjon("hentSnapshot", kontroll) { forbindelse ->
            setning(
                forbindelse,
                "SELECT ${dialekt.tidspunkt} AS RUNDESTART, MIN(TRANS_TILSTAND_ID) AS MIN_ID, MAX(TRANS_TILSTAND_ID) AS MAX_ID FROM T_TRANS_TILSTAND",
                kontroll,
            ).use { setning ->
                setning.executeQuery().use {
                    check(it.next())
                    Db2RyddeSnapshot(
                        it.getTimestamp("RUNDESTART").toLocalDateTime(),
                        (it.getObject("MIN_ID") as Number?)?.toInt(),
                        (it.getObject("MAX_ID") as Number?)?.toInt(),
                    )
                }
            }
        }

    override fun finnKandidater(
        fraId: Long,
        tilId: Long,
        regel: TilstandsRyddeRegel,
        grense: LocalDateTime,
        kontroll: RyddeEnhetskontroll,
    ): List<Int> {
        require(fraId in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong())
        require(tilId in fraId..Int.MAX_VALUE.toLong() && tilId - fraId < 10000)
        return transaksjon("finnKandidater", kontroll) { forbindelse ->
            setning(
                forbindelse,
                """
                SELECT TRANS_TILSTAND_ID FROM T_TRANS_TILSTAND
                WHERE TRANS_TILSTAND_ID BETWEEN ? AND ? AND ${regel.alderspredikat}
                ORDER BY TRANS_TILSTAND_ID
                """.trimIndent(),
                kontroll,
            ).use {
                it.setInt(1, fraId.toInt())
                it.setInt(2, tilId.toInt())
                it.setTimestamp(3, Timestamp.valueOf(grense))
                it.executeQuery().use { resultat ->
                    buildList { while (resultat.next()) add(resultat.getInt(1)) }
                }
            }
        }
    }

    override fun slettUrefererte(
        ider: List<Int>,
        regel: TilstandsRyddeRegel,
        grense: LocalDateTime,
        kontroll: RyddeEnhetskontroll,
    ): RyddeBatchresultat {
        kontrollerBatch(ider)
        if (ider.isEmpty()) return RyddeBatchresultat(0, 0)
        return transaksjon("slettUrefererte", kontroll) { forbindelse ->
            setning(
                forbindelse,
                """
                DELETE FROM T_TRANS_TILSTAND s
                WHERE s.TRANS_TILSTAND_ID IN (${plassholdere(ider)}) AND ${regel.alderspredikat}
                  AND NOT EXISTS (SELECT 1 FROM T_TRANSAKSJON t WHERE t.TRANS_TILSTAND_ID = s.TRANS_TILSTAND_ID)
                """.trimIndent(),
                kontroll,
            ).use {
                bindIderOgGrense(it, ider, grense)
                RyddeBatchresultat(it.executeUpdate(), 0)
            }
        }
    }

    override fun nullstillOgSlett(
        ider: List<Int>,
        regel: TilstandsRyddeRegel,
        grense: LocalDateTime,
        maksReferanser: Int,
        kontroll: RyddeEnhetskontroll,
    ): RyddeBatchresultat {
        kontrollerBatch(ider)
        require(maksReferanser in 1 until Int.MAX_VALUE)
        if (ider.isEmpty()) return RyddeBatchresultat(0, 0)
        return transaksjon("nullstillOgSlett", kontroll) { forbindelse ->
            // Db2 z/OS-syntaks og låsevirkning må godkjennes i DBA-testen; H2 beviser bare atomisiteten.
            val bekreftede = setning(
                forbindelse,
                """
                SELECT TRANS_TILSTAND_ID FROM T_TRANS_TILSTAND
                WHERE TRANS_TILSTAND_ID IN (${plassholdere(ider)}) AND ${regel.alderspredikat}
                ${dialekt.lasing}
                """.trimIndent(),
                kontroll,
            ).use {
                bindIderOgGrense(it, ider, grense)
                it.executeQuery().use { resultat ->
                    buildList { while (resultat.next()) add(resultat.getInt(1)) }
                }
            }
            if (bekreftede.isEmpty()) return@transaksjon RyddeBatchresultat(0, 0)

            val referanseantall = setning(
                forbindelse,
                """
                SELECT TRANSAKSJON_ID FROM T_TRANSAKSJON
                WHERE TRANS_TILSTAND_ID IN (${plassholdere(bekreftede)})
                FETCH FIRST ${maksReferanser + 1} ROWS ONLY
                """.trimIndent(),
                kontroll,
            ).use {
                bindIder(it, bekreftede)
                it.executeQuery().use { resultat ->
                    var antall = 0
                    while (resultat.next()) antall++
                    antall
                }
            }
            if (referanseantall > maksReferanser) throw ReferansegrenseOverskredet()

            val nullstilte = setning(
                forbindelse,
                "UPDATE T_TRANSAKSJON SET TRANS_TILSTAND_ID = NULL WHERE TRANS_TILSTAND_ID IN (${plassholdere(bekreftede)})",
                kontroll,
            ).use {
                bindIder(it, bekreftede)
                it.executeUpdate()
            }
            if (nullstilte > maksReferanser) throw ReferansegrenseOverskredet()

            val slettede = setning(
                forbindelse,
                "DELETE FROM T_TRANS_TILSTAND WHERE TRANS_TILSTAND_ID IN (${plassholdere(bekreftede)}) AND ${regel.alderspredikat}",
                kontroll,
            ).use {
                bindIderOgGrense(it, bekreftede, grense)
                it.executeUpdate()
            }
            if (slettede != bekreftede.size) throw RyddeSletteavvik()
            RyddeBatchresultat(slettede, nullstilte)
        }
    }

    private fun kontrollerBatch(ider: List<Int>) {
        require(ider.size <= 100 && ider.distinct().size == ider.size)
    }

    private fun plassholdere(ider: List<Int>): String = ider.joinToString { "?" }

    private fun bindIder(setning: PreparedStatement, ider: List<Int>) {
        ider.forEachIndexed { indeks, id -> setning.setInt(indeks + 1, id) }
    }

    private fun bindIderOgGrense(setning: PreparedStatement, ider: List<Int>, grense: LocalDateTime) {
        bindIder(setning, ider)
        setning.setTimestamp(ider.size + 1, Timestamp.valueOf(grense))
    }

    private fun setning(forbindelse: Connection, sql: String, kontroll: RyddeEnhetskontroll): PreparedStatement {
        val timeout = kontroll.timeoutSekunder()
        require(timeout > 0)
        return forbindelse.prepareStatement(sql).also { it.queryTimeout = timeout }
    }

    private fun <T> transaksjon(metode: String, kontroll: RyddeEnhetskontroll, handling: (Connection) -> T): T =
        Metrics.timer(DATABASE_CALL, "TilstandsRyddeRepository", metode).recordCallable {
            kontroll.timeoutSekunder()
            var bekreftetCommit = false
            try {
                dataSource.connection.use { forbindelse ->
                    forbindelse.autoCommit = false
                    try {
                        val resultat = handling(forbindelse)
                        kontroll.timeoutSekunder()
                        forbindelse.commit()
                        bekreftetCommit = true
                        resultat
                    } catch (feil: Exception) {
                        try {
                            forbindelse.rollback()
                        } catch (rollbackFeil: Exception) {
                            feil.addSuppressed(rollbackFeil)
                            throw RyddeUavklartUtfall(feil)
                        }
                        throw feil
                    }
                }
            } catch (feil: Exception) {
                if (bekreftetCommit) throw RyddeUavklartUtfall(feil)
                throw feil
            }
        }!!
}
