package no.nav.sokos.spk.mottak.repository

import java.time.Instant
import java.time.LocalDateTime

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe

import no.nav.sokos.spk.mottak.listener.PostgresListener

class TilstandsRyddeStatusRepositoryTest : FunSpec({
    extensions(PostgresListener)

    test("sesjonslås hindrer andre instanser og frigjøres eksplisitt") {
        val lager = TilstandsRyddeStatusRepository(PostgresListener.dataSource)
        val annen = TilstandsRyddeStatusRepository(PostgresListener.dataSource)
        lager.provKjoringslas()!!.use { las ->
            las.kontroller()
            annen.provKjoringslas() shouldBe null
        }
        annen.provKjoringslas()!!.use { it.kontroller() }
    }
    test("singleton bevarer mikrosekunder og sist fullført når ny runde lagres") {
        val lager = TilstandsRyddeStatusRepository(PostgresListener.dataSource)
        val sist = Instant.parse("2026-10-12T06:00:00Z")
        val status = TilstandsRyddeStatus(
            "seks-maaneder-v1", 6, LocalDateTime.parse("2026-04-12T08:00:00.123456"),
            1, Int.MAX_VALUE.toLong(), null, false, sist,
        )
        lager.provKjoringslas()!!.use { las ->
            lager.lagre(las, status)
            TilstandsRyddeStatusRepository(PostgresListener.dataSource).hent(las) shouldBe status
            lager.lagre(las, status.copy(forsteId = 2))
            lager.hent(las)!!.sisteFullfortTidspunkt shouldBe sist
            PostgresListener.dataSource.connection.use { tilkobling ->
                tilkobling.createStatement().use { setning ->
                    setning.executeQuery("SELECT count(*) FROM tilstandsrydding_status").use {
                        it.next()
                        it.getInt(1) shouldBe 1
                    }
                    setning.executeQuery(
                        "SELECT has_table_privilege(current_database() || '-user', 'tilstandsrydding_status', 'SELECT,INSERT,UPDATE')",
                    ).use {
                        it.next()
                        it.getBoolean(1) shouldBe true
                    }
                }
            }
        }
    }
})
