package no.nav.sokos.spk.mottak.repository

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.SQLException
import java.sql.SQLTimeoutException
import java.time.LocalDateTime
import java.util.UUID
import javax.sql.DataSource

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.spk.mottak.domain.SeksMaanedersRegel

class TilstandsRyddeRepositoryTest : FunSpec({
    val grense = LocalDateTime.parse("2026-04-12T08:00:00.123456")
    val regel = SeksMaanedersRegel()

    test("fase én skriver aldri til parent og støtter anvisere og manglende transaksjon") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.tilstand(2, grense.minusDays(1))
            db.parent(10, 2, "ANNEN")
            val tidligere = db.parentverdier()
            db.repository.slettUrefererte(listOf(1, 2), regel, grense, db.kontroll) shouldBe RyddeBatchresultat(1, 0)
            db.parentverdier() shouldBe tidligere
            db.antall("T_RETUR_TIL_ANV") shouldBe 1
            db.antall("T_TRANS_TILSTAND") shouldBe 1
        }
    }
    test("fase to nullstiller alle referanser atomisk og bevarer metadata og retur") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.tilstand(2, grense)
            db.parent(10, 1)
            db.parent(11, 1, "ANNEN")
            db.parent(12, 2)
            val metadata = db.metadata()
            db.repository.nullstillOgSlett(listOf(1, 2, 99), regel, grense, 200, db.kontroll) shouldBe RyddeBatchresultat(1, 2)
            db.metadata() shouldBe metadata
            db.referanser() shouldBe listOf(null, null, 2)
            db.antall("T_RETUR_TIL_ANV") shouldBe 3
            db.timeouter.all { it == 3 } shouldBe true
        }
    }
    test("endret alder og nyere parentpeker etter kandidatlesing beholdes") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.tilstand(2, grense.plusDays(1))
            db.parent(10, 1)
            val kandidater = db.repository.finnKandidater(1, 2, regel, grense, db.kontroll)
            db.sql("UPDATE T_TRANS_TILSTAND SET DATO_ENDRET = TIMESTAMP '$grense' WHERE TRANS_TILSTAND_ID = 1")
            db.sql("UPDATE T_TRANSAKSJON SET TRANS_TILSTAND_ID = 2 WHERE TRANSAKSJON_ID = 10")
            db.repository.nullstillOgSlett(kandidater, regel, grense, 200, db.kontroll) shouldBe RyddeBatchresultat(0, 0)
            db.referanser() shouldBe listOf(2)
            db.antall("T_TRANS_TILSTAND") shouldBe 2
        }
    }
    test("tomme og manglende id-er gir ingen parentendring") {
        RyddeDb().use { db ->
            db.repository.slettUrefererte(emptyList(), regel, grense, db.kontroll) shouldBe RyddeBatchresultat(0, 0)
            db.repository.nullstillOgSlett(emptyList(), regel, grense, 200, db.kontroll) shouldBe RyddeBatchresultat(0, 0)
            db.repository.nullstillOgSlett(listOf(99), regel, grense, 200, db.kontroll) shouldBe RyddeBatchresultat(0, 0)
        }
    }
    test("referansegrensen gir rollback uten å nullstille noen parent") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.parent(10, 1)
            db.parent(11, 1)
            shouldThrow<ReferansegrenseOverskredet> {
                db.repository.nullstillOgSlett(listOf(1), regel, grense, 1, db.kontroll)
            }
            db.referanser() shouldBe listOf(1, 1)
            db.antall("T_TRANS_TILSTAND") shouldBe 1
        }
    }
    test("nye referanser mellom probe og update begrenses av faktisk updateantall") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.parent(10, 1)
            db.forUtforelse = { sql, forbindelse ->
                if (sql.startsWith("UPDATE T_TRANSAKSJON")) {
                    forbindelse.createStatement().use { it.executeUpdate("INSERT INTO T_TRANSAKSJON VALUES (11,1,'SPK',TIMESTAMP '2020-01-01 00:00:00','urørt',17,123)") }
                }
            }
            shouldThrow<ReferansegrenseOverskredet> {
                db.repository.nullstillOgSlett(listOf(1), regel, grense, 1, db.kontroll)
            }
            db.referanser() shouldBe listOf(1)
            db.antall("T_TRANS_TILSTAND") shouldBe 1
        }
    }
    test("ukjent fremmednøkkel ruller tilbake etter nullstilling med ekte constraints") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.parent(10, 1)
            db.sql("CREATE TABLE EKSTRA (ID INT PRIMARY KEY, TILSTAND INT REFERENCES T_TRANS_TILSTAND)")
            db.sql("INSERT INTO EKSTRA VALUES (1,1)")
            shouldThrow<SQLException> {
                db.repository.nullstillOgSlett(listOf(1), regel, grense, 200, db.kontroll)
            }
            db.referanser() shouldBe listOf(1)
            db.antall("T_TRANS_TILSTAND") shouldBe 1
        }
    }
    test("sletteavvik gir rollback av nullstilling") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.parent(10, 1)
            db.forUtforelse = { sql, forbindelse ->
                if (sql.startsWith("DELETE")) {
                    forbindelse.createStatement().use {
                        it.executeUpdate("UPDATE T_TRANS_TILSTAND SET DATO_ENDRET = TIMESTAMP '$grense' WHERE TRANS_TILSTAND_ID = 1")
                    }
                }
            }
            shouldThrow<RyddeSletteavvik> {
                db.repository.nullstillOgSlett(listOf(1), regel, grense, 200, db.kontroll)
            }
            db.referanser() shouldBe listOf(1)
            db.repository.finnKandidater(1, 1, regel, grense, db.kontroll) shouldBe listOf(1)
        }
    }
    test("budsjett kontrolleres på nytt mellom setninger og rollback bevarer referansene") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.parent(10, 1)
            var kall = 0
            shouldThrow<IllegalStateException> {
                db.repository.nullstillOgSlett(listOf(1), regel, grense, 200, RyddeEnhetskontroll {
                    if (++kall >= 5) error("Stopp")
                    1
                })
            }
            db.referanser() shouldBe listOf(1)
            db.antall("T_TRANS_TILSTAND") shouldBe 1
        }
    }
    test("rollbackfeil bevarer begge JDBC-feil og markerer utfallet som uavklart") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.parent(10, 1)
            val opprinnelig = SQLException("sensitiv", "40001")
            val rollback = SQLException("rollback", "08006")
            db.forUtforelse = { sql, _ -> if (sql.startsWith("UPDATE")) throw opprinnelig }
            db.rollbackFeil = rollback
            val feil = shouldThrow<RyddeUavklartUtfall> {
                db.repository.nullstillOgSlett(listOf(1), regel, grense, 200, db.kontroll)
            }
            feil.cause shouldBe opprinnelig
            feil.cause!!.suppressed.toList() shouldBe listOf(rollback)
            db.referanser() shouldBe listOf(1)
        }
    }
    test("driver-timeout etter nullstilling ruller tilbake hele batchen") {
        RyddeDb().use { db ->
            db.tilstand(1, grense.minusDays(1))
            db.parent(10, 1)
            db.forUtforelse = { sql, _ ->
                if (sql.startsWith("DELETE")) throw SQLTimeoutException("sensitiv", "57014")
            }
            shouldThrow<SQLTimeoutException> {
                db.repository.nullstillOgSlett(listOf(1), regel, grense, 200, db.kontroll)
            }
            db.referanser() shouldBe listOf(1)
            db.antall("T_TRANS_TILSTAND") shouldBe 1
            db.timeouter.all { it == 3 } shouldBe true
        }
    }
})

internal class RyddeDb : AutoCloseable {
    val dataSource = HikariDataSource(HikariConfig().apply {
        jdbcUrl = "jdbc:h2:mem:rydding_${UUID.randomUUID()};MODE=DB2"
        maximumPoolSize = 3
    })
    val timeouter = mutableListOf<Int>()
    val kontroll = RyddeEnhetskontroll { 3 }
    var forUtforelse: (String, Connection) -> Unit = { _, _ -> }
    var rollbackFeil: SQLException? = null
    val repository = TilstandsRyddeRepository(
        object : DataSource by dataSource {
            override fun getConnection(): Connection {
                val forbindelse = dataSource.connection
                return Proxy.newProxyInstance(Connection::class.java.classLoader, arrayOf(Connection::class.java)) { _, metode, argumenter ->
                    try {
                        if (metode.name == "rollback") rollbackFeil?.let { throw it }
                        val resultat = metode.invoke(forbindelse, *(argumenter ?: emptyArray()))
                        if (metode.name != "prepareStatement") resultat else {
                            val sql = argumenter!![0] as String
                            val setning = resultat as PreparedStatement
                            Proxy.newProxyInstance(PreparedStatement::class.java.classLoader, arrayOf(PreparedStatement::class.java)) { _, kall, verdier ->
                                try {
                                    if (kall.name.startsWith("execute")) {
                                        timeouter += setning.queryTimeout
                                        forUtforelse(sql, forbindelse)
                                    }
                                    kall.invoke(setning, *(verdier ?: emptyArray()))
                                } catch (feil: InvocationTargetException) {
                                    throw feil.targetException
                                }
                            }
                        }
                    } catch (feil: InvocationTargetException) {
                        throw feil.targetException
                    }
                } as Connection
            }
        },
        RyddeSqlDialekt.H2,
    )

    init {
        sql("CREATE TABLE T_TRANSAKSJON (TRANSAKSJON_ID INT PRIMARY KEY, TRANS_TILSTAND_ID INT, K_ANVISER VARCHAR(20), DATO_ENDRET TIMESTAMP(6), ENDRET_AV VARCHAR(50), VERSJON INT, BELOP INT)")
        sql("CREATE TABLE T_TRANS_TILSTAND (TRANS_TILSTAND_ID INT PRIMARY KEY, DATO_ENDRET TIMESTAMP(6) NOT NULL, TRANSAKSJON_ID INT REFERENCES T_TRANSAKSJON)")
        sql("ALTER TABLE T_TRANSAKSJON ADD FOREIGN KEY (TRANS_TILSTAND_ID) REFERENCES T_TRANS_TILSTAND")
        sql("CREATE INDEX XIF12T_TRANSAKSJON ON T_TRANSAKSJON(TRANS_TILSTAND_ID)")
        sql("CREATE TABLE T_RETUR_TIL_ANV (ID INT PRIMARY KEY, TRANSAKSJON_ID INT REFERENCES T_TRANSAKSJON)")
    }

    fun sql(sql: String) = dataSource.connection.use { it.createStatement().use { setning -> setning.execute(sql) } }

    fun tilstand(id: Int, dato: LocalDateTime) {
        sql("INSERT INTO T_TRANS_TILSTAND (TRANS_TILSTAND_ID,DATO_ENDRET,TRANSAKSJON_ID) VALUES ($id, TIMESTAMP '$dato', NULL)")
    }

    fun parent(id: Int, tilstand: Int?, anviser: String = "SPK") {
        sql("INSERT INTO T_TRANSAKSJON (TRANSAKSJON_ID,TRANS_TILSTAND_ID,K_ANVISER,DATO_ENDRET,ENDRET_AV,VERSJON,BELOP) VALUES ($id,${tilstand ?: "NULL"},'$anviser',TIMESTAMP '2020-01-01 00:00:00','urørt',17,123)")
        sql("INSERT INTO T_RETUR_TIL_ANV VALUES ($id,$id)")
    }

    fun antall(tabell: String): Int = dataSource.connection.use {
        it.createStatement().use { setning ->
            setning.executeQuery("SELECT count(*) FROM $tabell").use { resultat -> resultat.next(); resultat.getInt(1) }
        }
    }

    fun parentverdier(): List<List<Any?>> = verdier("SELECT * FROM T_TRANSAKSJON ORDER BY TRANSAKSJON_ID")

    fun metadata(): List<List<Any?>> = verdier("SELECT TRANSAKSJON_ID,K_ANVISER,DATO_ENDRET,ENDRET_AV,VERSJON,BELOP FROM T_TRANSAKSJON ORDER BY TRANSAKSJON_ID")

    fun referanser(): List<Int?> = verdier("SELECT TRANS_TILSTAND_ID FROM T_TRANSAKSJON ORDER BY TRANSAKSJON_ID").map { it[0] as Int? }

    private fun verdier(sql: String): List<List<Any?>> = dataSource.connection.use {
        it.createStatement().use { setning ->
            setning.executeQuery(sql).use { resultat ->
                buildList {
                    while (resultat.next()) add((1..resultat.metaData.columnCount).map { kolonne -> resultat.getObject(kolonne) })
                }
            }
        }
    }

    override fun close() = dataSource.close()
}
