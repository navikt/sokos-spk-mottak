package no.nav.sokos.spk.mottak.service

import java.sql.SQLException
import java.time.Duration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain

import no.nav.sokos.spk.mottak.config.TilstandsRyddeKonfigurasjon
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.RyddeDb
import no.nav.sokos.spk.mottak.repository.RyddeLasTapt

class TilstandsRyddeIntegrasjonTest :
    FunSpec({
        test("ekte rollback etter nullstilling beholder markør og committed-baserte målinger") {
            RyddeDb().use { db ->
                val f = RyddeServiceFikstur()
                val grense = f.tid.minusMonths(6)
                f.lager.status = f.status(1, 2, null)
                db.tilstand(1, grense.minusDays(1))
                db.tilstand(2, grense.minusDays(1))
                db.parent(10, 2)
                db.sql("CREATE TABLE EKSTRA (ID INT PRIMARY KEY, TILSTAND INT REFERENCES T_TRANS_TILSTAND)")
                db.sql("INSERT INTO EKSTRA VALUES (1,2)")
                val slettedeFor = Metrics.tilstandsryddingSlettede.labelValues("urefererte").get()
                val resterendeFor = Metrics.tilstandsryddingSlettede.labelValues("resterende").get()
                val referanserFor = Metrics.tilstandsryddingNullstilteReferanser.get()
                val runderFor = Metrics.tilstandsryddingRunderFullfort.get()
                val service =
                    TilstandsRyddeService(
                        db.repository,
                        f.lager.repository,
                        TilstandsRyddeKonfigurasjon(intervallBredde = 2),
                        klokke = f.klokke,
                        nanoTime = { 0L },
                        vent = { db.dataSource.hikariPoolMXBean.activeConnections shouldBe 0 },
                        db2DataSource = db.transactionDataSource,
                    )
                shouldThrow<SQLException> { service.kjor() }
                db.antall("T_TRANS_TILSTAND") shouldBe 1
                db.referanser() shouldBe listOf(2)
                f.lager.status!!.sisteFerdigeId shouldBe null
                Metrics.tilstandsryddingSlettede.labelValues("urefererte").get() - slettedeFor shouldBe 1.0
                Metrics.tilstandsryddingSlettede.labelValues("resterende").get() shouldBe resterendeFor
                Metrics.tilstandsryddingNullstilteReferanser.get() shouldBe referanserFor
                Metrics.tilstandsryddingRunderFullfort.get() shouldBe runderFor
                db.sql("DELETE FROM EKSTRA")
                f.lager.las = MinneKjoringslas()
                service.kjor().slettede shouldBe 1L
                Metrics.tilstandsryddingNullstilteReferanser.get() - referanserFor shouldBe 1.0
                Metrics.tilstandsryddingRunderFullfort.get() - runderFor shouldBe 1.0
                f.lager.status!!.fullfort shouldBe true
            }
        }
        test("advisory-sesjonstap etter commit stopper før markør og replay teller ikke sletting igjen") {
            RyddeDb().use { db ->
                val f = RyddeServiceFikstur()
                val grense = f.tid.minusMonths(6)
                f.lager.status = f.status(1, 1, null)
                db.tilstand(1, grense.minusDays(1))
                val forrige = Metrics.tilstandsryddingSlettede.labelValues("urefererte").get()
                var ventinger = 0
                val service =
                    TilstandsRyddeService(
                        db.repository,
                        f.lager.repository,
                        TilstandsRyddeKonfigurasjon(),
                        nanoTime = { 0L },
                        vent = {
                            db.dataSource.hikariPoolMXBean.activeConnections shouldBe 0
                            if (++ventinger == 2) f.lager.las.tapt = true
                        },
                        db2DataSource = db.transactionDataSource,
                    )
                shouldThrow<RyddeLasTapt> { service.kjor() }
                f.lager.status!!.sisteFerdigeId shouldBe null
                Metrics.tilstandsryddingSlettede.labelValues("urefererte").get() - forrige shouldBe 1.0
                f.lager.las = MinneKjoringslas()
                service.kjor().slettede shouldBe 0L
                Metrics.tilstandsryddingSlettede.labelValues("urefererte").get() - forrige shouldBe 1.0
            }
        }
        test("Prometheus bruker eksisterende registry med avgrensede etiketter og gjenopprettet fullføringstid") {
            val f = RyddeServiceFikstur()
            val sist = f.klokke.instant().minusSeconds(100)
            f.lager.status = f.status(1, 2, null).copy(sisteFullfortTidspunkt = sist)
            f.db.feilVed = "kandidater"
            f.db.feil = SQLException("Personopplysninger skal ikke eksporteres", "23503")
            shouldThrow<SQLException> { f.service().kjor() }
            Metrics.tilstandsryddingSisteFullfort.get() shouldBe sist.epochSecond.toDouble()
            val prometheus = Metrics.prometheusMeterRegistry.scrape()
            listOf(
                "kandidater_total",
                "slettede_total",
                "nullstilte_referanser_total",
                "sqlfeil_total",
                "kjoringer_total",
                "runder_fullfort_total",
                "pause_sekunder",
                "markor_id",
                "ovre_id",
                "aktiv",
                "siste_fullfort_timestamp_sekunder",
            ).forEach { prometheus shouldContain "sokos_spk_mottak_tilstandsrydding_$it" }
            prometheus shouldContain "fase=\"urefererte\""
            prometheus shouldContain "fase=\"resterende\""
            prometheus shouldContain "type=\"fremmednokkel\""
            prometheus shouldContain "utfall=\"feil\""
            prometheus shouldContain "className=\"TilstandsRyddeService\""
            prometheus.contains("Personopplysninger") shouldBe false
            Metrics.tilstandsryddingAktiv.get() shouldBe 0.0
        }
        test("ukjent SQLSTATE i samme kjede som låsefeil gir ingen retry") {
            val f = RyddeServiceFikstur()
            f.db.feilVed = "snapshot"
            f.db.feil = SQLException("ukjent", "08006").apply { nextException = SQLException("lås", "40001") }
            shouldThrow<SQLException> { f.service().kjor() }
            f.db.kall.size shouldBe 1
        }
        test("interruption av ventingen bevarer commits og interruptflagget") {
            val f = RyddeServiceFikstur()
            f.lager.status = f.status(1, 2, null)
            val service =
                TilstandsRyddeService(
                    f.db.repository,
                    f.lager.repository,
                    TilstandsRyddeKonfigurasjon(),
                    nanoTime = { 0L },
                    vent = { if (f.db.kall.last() == "urefererte") throw InterruptedException() },
                    db2DataSource = ryddeTestDataSource,
                )
            try {
                service.kjor().slettede shouldBe 1L
                Thread.currentThread().isInterrupted shouldBe true
                f.lager.status!!.sisteFerdigeId shouldBe null
            } finally {
                Thread.interrupted()
            }
        }
        test("ny runde beholder tidligere fullføringstid når budsjettet stopper før sletting") {
            val f = RyddeServiceFikstur()
            val sist = f.klokke.instant().minusSeconds(100)
            f.lager.status = f.status(1, 2, 2).copy(fullfort = true, sisteFullfortTidspunkt = sist)
            f.etterVent = { if (f.db.kall.last() == "kandidater") f.nanos = Duration.ofSeconds(10).toNanos() }
            f.service().kjor()
            f.lager.status!!.sisteFullfortTidspunkt shouldBe sist
            Metrics.tilstandsryddingSisteFullfort.get() shouldBe sist.epochSecond.toDouble()
        }
    })
