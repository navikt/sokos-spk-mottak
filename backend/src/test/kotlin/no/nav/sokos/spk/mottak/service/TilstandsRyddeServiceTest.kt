package no.nav.sokos.spk.mottak.service

import java.sql.SQLException
import java.sql.SQLTimeoutException
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk

import no.nav.sokos.spk.mottak.config.TilstandsRyddeKonfigurasjon
import no.nav.sokos.spk.mottak.domain.RyddeStoppgrunn
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.Db2RyddeSnapshot
import no.nav.sokos.spk.mottak.repository.Kjoringslas
import no.nav.sokos.spk.mottak.repository.RyddeBatchresultat
import no.nav.sokos.spk.mottak.repository.RyddeEnhetskontroll
import no.nav.sokos.spk.mottak.repository.RyddeLasTapt
import no.nav.sokos.spk.mottak.repository.RyddeUavklartUtfall
import no.nav.sokos.spk.mottak.repository.TilstandsRyddeRepository
import no.nav.sokos.spk.mottak.repository.TilstandsRyddeStatus
import no.nav.sokos.spk.mottak.repository.TilstandsRyddeStatusRepository

internal val ryddeTestDataSource =
    HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:rydding_service_test;MODE=DB2;DB_CLOSE_DELAY=-1"
            maximumPoolSize = 2
            minimumIdle = 0
        },
    )

class TilstandsRyddeServiceTest :
    FunSpec({
        test("fullført lagret runde forblir fullført når låsefrigjøring passerer budsjettet") {
            val f = RyddeServiceFikstur()
            val lager = f.lager.repository
            every { lager.provKjoringslas(any()) } returns
                object : Kjoringslas {
                    override fun kontroller(timeoutSekunder: Int) {}

                    override fun close() {
                        f.nanos = f.maksKjoretid.toNanos() + 1
                    }
                }
            val resultat =
                TilstandsRyddeService(
                    f.db.repository,
                    lager,
                    TilstandsRyddeKonfigurasjon(maksKjoretid = f.maksKjoretid),
                    nanoTime = { f.nanos },
                    vent = {},
                    db2DataSource = ryddeTestDataSource,
                ).kjor()
            resultat.stoppgrunn shouldBe RyddeStoppgrunn.FULLFORT
            f.lager.status!!.fullfort shouldBe true
        }
        test("avbrutt retryventing rapporterer avbrudd med original SQL som kontekst") {
            val f = RyddeServiceFikstur()
            f.db.feilVed = "snapshot"
            f.db.feil = SQLException("sensitiv", "40001")
            val resultat =
                TilstandsRyddeService(
                    f.db.repository,
                    f.lager.repository,
                    nanoTime = { 0L },
                    vent = { throw InterruptedException() },
                    db2DataSource = ryddeTestDataSource,
                )
            try {
                resultat.kjor().stoppgrunn shouldBe RyddeStoppgrunn.AVBRUTT
                f.db.kall.size shouldBe 1
            } finally {
                Thread.interrupted()
            }
        }
        test("budsjettstopp i tillatt retry bevarer SQL-kontekst uten å bli feil") {
            val f = RyddeServiceFikstur()
            f.db.feilVed = "snapshot"
            f.db.feil = SQLException("sensitiv", "40001")
            val stopp =
                no.nav.sokos.spk.mottak.domain
                    .RyddeStopp(RyddeStoppgrunn.BUDSJETT)
            val service =
                TilstandsRyddeService(
                    f.db.repository,
                    f.lager.repository,
                    nanoTime = { 0L },
                    vent = { throw stopp },
                    db2DataSource = ryddeTestDataSource,
                )
            service.kjor().stoppgrunn shouldBe RyddeStoppgrunn.BUDSJETT
            stopp.suppressed.toList() shouldBe listOf(f.db.feil)
            f.db.kall.size shouldBe 1
        }
        test("uttømte eller ukjente SQL-feil forblir feil selv om pause avbrytes") {
            for (sqlstate in listOf("40001", "99999")) {
                val f = RyddeServiceFikstur()
                f.db.feilVed = "snapshot"
                f.db.feil = SQLException("sensitiv", sqlstate)
                val service =
                    TilstandsRyddeService(
                        f.db.repository,
                        f.lager.repository,
                        TilstandsRyddeKonfigurasjon(maksForsok = 1),
                        nanoTime = { 0L },
                        vent = { throw InterruptedException() },
                        db2DataSource = ryddeTestDataSource,
                    )
                try {
                    shouldThrow<SQLException> { service.kjor() } shouldBe f.db.feil
                    (
                        f.db.feil.suppressed
                            .single() is no.nav.sokos.spk.mottak.domain.RyddeStopp
                    ) shouldBe true
                } finally {
                    Thread.interrupted()
                }
            }
        }
        test("stopp med feil i close feiler synlig og teller frigjøringsfeilen én gang") {
            val f = RyddeServiceFikstur()
            val cleanupFeil = SQLException("sensitiv close", "08006")
            val observerte = mutableListOf<RyddeStoppgrunn>()
            val lager = f.lager.repository
            every { lager.provKjoringslas(any()) } returns
                object : Kjoringslas {
                    override fun kontroller(timeoutSekunder: Int) {}

                    override fun close(): Unit = throw cleanupFeil
                }
            f.lager.etterHent = { f.nanos = f.maksKjoretid.toNanos() }
            val forrige = Metrics.tilstandsryddingSqlfeil.labelValues("annen").get()
            val service =
                TilstandsRyddeService(
                    f.db.repository,
                    lager,
                    TilstandsRyddeKonfigurasjon(maksKjoretid = f.maksKjoretid),
                    nanoTime = { f.nanos },
                    vent = {},
                    avslutningsobservator = { observerte += it.stoppgrunn },
                    db2DataSource = ryddeTestDataSource,
                )
            shouldThrow<SQLException> { service.kjor() } shouldBe cleanupFeil
            (cleanupFeil.suppressed.single() is no.nav.sokos.spk.mottak.domain.RyddeStopp) shouldBe true
            observerte shouldBe listOf(RyddeStoppgrunn.FEIL)
            Metrics.tilstandsryddingSqlfeil.labelValues("annen").get() - forrige shouldBe 1.0
        }
        test("Postgres-feil i anskaffelse lesing lagring helse og close telles én gang") {
            for (operasjon in listOf("anskaffelse", "hent", "lagre", "helse", "close")) {
                val f = RyddeServiceFikstur()
                val feil = SQLException("sensitiv", "08006")
                val las =
                    object : Kjoringslas {
                        override fun kontroller(timeoutSekunder: Int) {
                            if (operasjon == "helse") throw feil
                        }

                        override fun close() {
                            if (operasjon == "close") throw feil
                        }
                    }
                val lager = mockk<TilstandsRyddeStatusRepository>()
                every { lager.provKjoringslas(any()) } answers {
                    if (operasjon == "anskaffelse") throw feil
                    las
                }
                every { lager.hent(any(), any()) } answers {
                    if (operasjon == "hent") throw feil
                    null
                }
                every { lager.lagre(any(), any(), any()) } answers {
                    if (operasjon == "lagre") throw feil
                }
                val forrige = Metrics.tilstandsryddingSqlfeil.labelValues("annen").get()
                shouldThrow<SQLException> {
                    TilstandsRyddeService(f.db.repository, lager, nanoTime = { 0L }, vent = {}, db2DataSource = ryddeTestDataSource).kjor()
                } shouldBe feil
                Metrics.tilstandsryddingSqlfeil.labelValues("annen").get() - forrige shouldBe 1.0
            }
        }
        test("alle utfall rapporterer trygg aggregert oppsummering og originalfeilen beholdes") {
            for (utfall in listOf("fullfort", "opptatt", "avbrutt", "budsjett", "feil")) {
                val f = RyddeServiceFikstur()
                val observerte = mutableListOf<no.nav.sokos.spk.mottak.domain.TilstandsRyddeOppsummering>()
                f.lager.opptatt = utfall == "opptatt"
                f.stopp = utfall == "avbrutt"
                if (utfall == "budsjett") f.etterVent = { f.nanos = f.maksKjoretid.toNanos() }
                if (utfall == "feil") f.db.feilVed = "snapshot"
                val service =
                    TilstandsRyddeService(
                        f.db.repository,
                        f.lager.repository,
                        TilstandsRyddeKonfigurasjon(maksKjoretid = f.maksKjoretid),
                        nanoTime = { f.nanos },
                        vent = { f.etterVent() },
                        stoppindikator = { f.stopp },
                        avslutningsobservator = { observerte += it },
                        db2DataSource = ryddeTestDataSource,
                    )
                if (utfall == "feil") {
                    shouldThrow<SQLException> { service.kjor() } shouldBe f.db.feil
                } else {
                    service.kjor().stoppgrunn.utfall shouldBe utfall
                }
                observerte.single().stoppgrunn.utfall shouldBe utfall
                observerte.single().slettede shouldBe if (utfall == "fullfort") 2L else 0L
            }
        }
        test("utløpt budsjett etter PostgreSQL-lesing stopper før statusgauges og Db2") {
            val f = RyddeServiceFikstur()
            f.lager.status = f.status(1, 2, null)
            f.lager.etterHent = { f.nanos = f.maksKjoretid.toNanos() }
            val forrige = Metrics.tilstandsryddingOvre.get()
            f.service().kjor().stoppgrunn shouldBe RyddeStoppgrunn.BUDSJETT
            f.db.kall shouldBe emptyList()
            Metrics.tilstandsryddingOvre.get() shouldBe forrige
        }
        test("feil i avslutningsobservator erstatter ikke opprinnelig databasefeil") {
            val f = RyddeServiceFikstur()
            f.db.feilVed = "snapshot"
            val observatorFeil = IllegalStateException("Observator")
            val service =
                TilstandsRyddeService(
                    f.db.repository,
                    f.lager.repository,
                    nanoTime = { 0L },
                    vent = {},
                    avslutningsobservator = { throw observatorFeil },
                    db2DataSource = ryddeTestDataSource,
                )
            shouldThrow<SQLException> { service.kjor() } shouldBe f.db.feil
            f.db.feil.suppressed
                .toList() shouldBe listOf(observatorFeil)
        }
        test("opptatt lås gir ingen Db2-kall") {
            val f = RyddeServiceFikstur()
            f.lager.opptatt = true
            f.service().kjor().stoppgrunn shouldBe RyddeStoppgrunn.OPPTATT
            f.db.kall shouldBe emptyList()
        }
        test("fase én er ferdig før andre kandidatlesing og fase to og alle enheter har pause") {
            val f = RyddeServiceFikstur()
            val resultat = f.service().kjor()
            f.db.kall shouldBe listOf("snapshot", "kandidater", "urefererte", "kandidater", "resterende")
            f.ventinger.size shouldBe f.db.kall.size
            resultat.slettede shouldBe 2L
            resultat.nullstilteReferanser shouldBe 1L
            resultat.stoppgrunn shouldBe RyddeStoppgrunn.FULLFORT
            f.lager.status!!.sisteFerdigeId shouldBe 2L
            f.lager.status!!.fullfort shouldBe true
            f.lager.las.lukket shouldBe true
        }
        test("tomt intervall har to kandidatlesinger og pauser") {
            val f = RyddeServiceFikstur()
            f.db.rader.clear()
            f.db.snapshot = Db2RyddeSnapshot(f.tid, 1, 2)
            f.service().kjor().stoppgrunn shouldBe RyddeStoppgrunn.FULLFORT
            f.db.kall shouldBe listOf("snapshot", "kandidater", "kandidater")
            f.ventinger.size shouldBe 3
        }
        test("tom kilde fullfører én runde med ikke-null grense og bevart fullføringstid") {
            val f = RyddeServiceFikstur()
            f.db.snapshot = Db2RyddeSnapshot(f.tid, null, null)
            f.db.rader.clear()
            f.service().kjor().stoppgrunn shouldBe RyddeStoppgrunn.FULLFORT
            f.lager.status!!.grensetidspunkt shouldBe f.tid.minusMonths(6)
            f.lager.status!!.forsteId shouldBe null
            f.lager.status!!.sisteFullfortTidspunkt shouldBe f.klokke.instant()
            f.db.kall shouldBe listOf("snapshot")
            f.ventinger.size shouldBe 1
        }
        test("tap av låsesesjonen stopper før neste Db2-enhet") {
            val f = RyddeServiceFikstur()
            f.etterVent = { f.lager.las.tapt = true }
            shouldThrow<RyddeLasTapt> { f.service().kjor() }
            f.db.kall shouldBe listOf("snapshot")
            f.lager.lagringer shouldBe 0
            f.lager.las.lukket shouldBe true
        }
        test("tidsbudsjett etter commit beholder opptalte commits men flytter ikke intervallet") {
            val f = RyddeServiceFikstur()
            f.etterVent = { if (f.db.kall.last() == "urefererte") f.nanos = Duration.ofSeconds(10).toNanos() }
            val resultat = f.service().kjor()
            resultat.stoppgrunn shouldBe RyddeStoppgrunn.BUDSJETT
            resultat.slettede shouldBe 1L
            f.lager.status!!.sisteFerdigeId shouldBe null
            f.db.kall shouldBe listOf("snapshot", "kandidater", "urefererte")
        }
        test("fase to-feil og feil i Postgres lar markøren stå og stopper videre Db2-arbeid") {
            val f = RyddeServiceFikstur()
            f.db.feilVed = "resterende"
            f.db.feil = SQLException("skal ikke logges", "23503")
            shouldThrow<SQLException> { f.service().kjor() }
            f.lager.status!!.sisteFerdigeId shouldBe null
            f.db.rader.keys
                .toList() shouldBe listOf(2)
            f.db.feilVed = null
            f.lager.feilVedLagring = f.lager.lagringer + 1
            shouldThrow<IllegalStateException> { f.service().kjor() }
            f.lager.status!!.sisteFerdigeId shouldBe null
            f.db.rader shouldBe emptyMap()
        }
        test("krasj etter Db2-commit gir idempotent omkjøring uten dobbel sletteteller") {
            val f = RyddeServiceFikstur()
            val forrige = Metrics.tilstandsryddingSlettede.labelValues("resterende").get()
            f.lager.feilVedLagring = 2
            shouldThrow<IllegalStateException> { f.service().kjor() }
            Metrics.tilstandsryddingSlettede.labelValues("resterende").get() - forrige shouldBe 1.0
            f.lager.feilVedLagring = null
            f.service().kjor().slettede shouldBe 0L
            Metrics.tilstandsryddingSlettede.labelValues("resterende").get() - forrige shouldBe 1.0
            f.db.kall.count { it == "snapshot" } shouldBe 1
        }
        test("ufullført runde beholder frosset grense og øvre id over ny serviceinstans") {
            val f = RyddeServiceFikstur()
            f.lager.status = f.status(1, 2, null)
            f.db.snapshot = Db2RyddeSnapshot(f.tid.plusYears(1), 1, 20)
            f.service().kjor().stoppgrunn shouldBe RyddeStoppgrunn.FULLFORT
            f.db.kall.contains("snapshot") shouldBe false
            f.db.grenser.distinct() shouldBe listOf(f.tid.minusMonths(6))
            f.lager.status!!.ovreId shouldBe 2L
        }
        test("endret regelversjon eller oppbevaring starter fra begynnelsen") {
            val f = RyddeServiceFikstur()
            f.lager.status = f.status(1, 2, 1).copy(regelversjon = "eldre-regel")
            f.service().kjor()
            f.db.kall.first() shouldBe "snapshot"
            f.lager.status!!.regelversjon shouldBe "seks-maaneder-v1"
            f.lager.status = f.status(1, 2, 1)
            f.db.kall.clear()
            f.service(oppbevaring = 12).kjor()
            f.db.kall.first() shouldBe "snapshot"
            f.lager.status!!.grensetidspunkt shouldBe f.tid.minusMonths(12)
        }
        test("sen lav id fanges av neste runde og heltallsgrensen overflyter ikke") {
            val f = RyddeServiceFikstur()
            f.db.snapshot = Db2RyddeSnapshot(f.tid, Int.MAX_VALUE - 1, Int.MAX_VALUE)
            f.db.rader.clear()
            f.db.rader[Int.MAX_VALUE] = false
            f.service().kjor().slettede shouldBe 1L
            f.lager.status!!.sisteFerdigeId shouldBe Int.MAX_VALUE.toLong()
            f.db.rader[1] = false
            f.db.snapshot = Db2RyddeSnapshot(f.tid, 1, 1)
            f.service().kjor().slettede shouldBe 1L
            f.db.kall.count { it == "snapshot" } shouldBe 2
        }
        test("bare godkjente låsefeil prøves igjen høyst totalt antall forsøk med maksimal pause") {
            val f = RyddeServiceFikstur()
            f.maksKjoretid = Duration.ofMinutes(2)
            f.lager.status = f.status(1, 2, null)
            f.db.feilVed = "kandidater"
            f.db.feil = SQLException("sensitiv", "40001")
            shouldThrow<SQLException> { f.service().kjor() }
            f.db.kall.count { it == "kandidater" } shouldBe 3
            f.ventinger shouldBe List(3) { Duration.ofSeconds(30) }
            f.lager.status!!.sisteFerdigeId shouldBe null
        }
        test("nestet nextException tillater låseretry men uavklart rollbackutfall gjør det ikke") {
            val f = RyddeServiceFikstur()
            f.lager.status = f.status(1, 2, null)
            f.db.feilVed = "kandidater"
            f.db.feil = SQLException("ytterste").apply { nextException = SQLException("innerste", "57033") }
            shouldThrow<SQLException> { f.service().kjor() }
            f.db.kall.size shouldBe 3
            f.db.kall.clear()
            f.db.feil = RyddeUavklartUtfall(SQLException("feil", "40001"))
            shouldThrow<RyddeUavklartUtfall> { f.service().kjor() }
            f.db.kall.size shouldBe 1
        }
        test("timeout og ukjente feil forsøkes ikke igjen og avbrudd gir stopp") {
            val f = RyddeServiceFikstur()
            f.db.feilVed = "snapshot"
            f.db.feil = SQLTimeoutException("sensitiv", "57014")
            shouldThrow<SQLTimeoutException> { f.service().kjor() }
            f.db.kall.size shouldBe 1
            f.db.kall.clear()
            f.db.feil = SQLException("sensitiv", "99999")
            shouldThrow<SQLException> { f.service().kjor() }
            f.db.kall.size shouldBe 1
            f.stopp = true
            f.db.kall.clear()
            f.service().kjor().stoppgrunn shouldBe RyddeStoppgrunn.AVBRUTT
            f.db.kall shouldBe emptyList()
        }
    })

internal class RyddeServiceFikstur {
    val tid = LocalDateTime.parse("2026-10-12T08:00:00.123456")
    val klokke = Clock.fixed(Instant.parse("2026-10-12T06:00:00Z"), ZoneOffset.UTC)
    val lager = MockRyddestatus()
    val db = MockRyddedata(Db2RyddeSnapshot(tid, 1, 2))
    var nanos = 0L
    var stopp = false
    var maksKjoretid = Duration.ofSeconds(10)
    val ventinger = mutableListOf<Duration>()
    var etterVent: () -> Unit = {}

    fun status(
        forste: Long,
        ovre: Long,
        siste: Long?,
    ) = TilstandsRyddeStatus("seks-maaneder-v1", 6, tid.minusMonths(6), forste, ovre, siste, false, null)

    fun service(oppbevaring: Int = 6): TilstandsRyddeService {
        lager.las = MinneKjoringslas()
        return TilstandsRyddeService(
            db.repository,
            lager.repository,
            TilstandsRyddeKonfigurasjon(
                oppbevaringMaaneder = oppbevaring,
                maksKjoretid = maksKjoretid,
                intervallBredde = 2,
            ),
            klokke = klokke,
            nanoTime = { nanos },
            vent = {
                ventinger += it
                etterVent()
            },
            stoppindikator = { stopp },
            db2DataSource = ryddeTestDataSource,
        )
    }
}

internal class MinneKjoringslas : Kjoringslas {
    var lukket = false
    var tapt = false

    override fun kontroller(timeoutSekunder: Int) {
        if (tapt || lukket) throw RyddeLasTapt()
    }

    override fun close() {
        lukket = true
    }
}

internal class MockRyddestatus {
    val repository = mockk<TilstandsRyddeStatusRepository>()
    var etterHent: () -> Unit = {}
    var las = MinneKjoringslas()
    var opptatt = false
    var status: TilstandsRyddeStatus? = null
    var lagringer = 0
    var feilVedLagring: Int? = null

    init {
        every { repository.provKjoringslas(any()) } answers { if (opptatt) null else las }
        every { repository.hent(any(), any()) } answers {
            arg<Kjoringslas>(0).kontroller()
            etterHent()
            status
        }
        every { repository.lagre(any(), any(), any()) } answers {
            arg<Kjoringslas>(0).kontroller()
            if (++lagringer == feilVedLagring) error("Postgres-feil")
            status = arg<TilstandsRyddeStatus>(1)
        }
    }
}

internal class MockRyddedata(
    var snapshot: Db2RyddeSnapshot,
) {
    val repository = mockk<TilstandsRyddeRepository>()
    val kall = mutableListOf<String>()
    val timeouter = mutableListOf<Int>()
    val grenser = mutableListOf<LocalDateTime>()
    val rader = linkedMapOf(1 to false, 2 to true)
    var feilVed: String? = null
    var feil: Exception = SQLException("Feil", "23503")

    init {
        every { repository.hentSnapshot(any(), any()) } answers {
            kall("snapshot", arg(1))
            snapshot
        }
        every { repository.finnKandidater(any(), any(), any(), any(), any(), any()) } answers {
            kall("kandidater", arg(5))
            val fraId = arg<Long>(1)
            val tilId = arg<Long>(2)
            grenser += arg<LocalDateTime>(4)
            rader.keys.filter { it.toLong() in fraId..tilId }
        }
        every { repository.slettUrefererte(any(), any(), any(), any(), any()) } answers {
            kall("urefererte", arg(4))
            val ider = arg<List<Int>>(1)
            val slettede = ider.filter { rader[it] == false }
            slettede.forEach(rader::remove)
            RyddeBatchresultat(slettede.size, 0)
        }
        every { repository.nullstillOgSlett(any(), any(), any(), any(), any(), any()) } answers {
            kall("resterende", arg(5))
            val ider = arg<List<Int>>(1)
            val referanser = ider.count { rader[it] == true }
            val slettede = ider.count { rader.containsKey(it) }
            ider.forEach(rader::remove)
            RyddeBatchresultat(slettede, referanser)
        }
    }

    private fun kall(
        navn: String,
        kontroll: RyddeEnhetskontroll,
    ) {
        timeouter += kontroll.timeoutSekunder()
        kall += navn
        if (feilVed == navn) throw feil
    }
}
