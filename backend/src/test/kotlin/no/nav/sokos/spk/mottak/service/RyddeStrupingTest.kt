package no.nav.sokos.spk.mottak.service

import java.time.Duration

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

import no.nav.sokos.spk.mottak.config.TilstandsRyddeKonfigurasjon
import no.nav.sokos.spk.mottak.domain.RyddeStopp
import no.nav.sokos.spk.mottak.domain.RyddeStoppgrunn

class RyddeStrupingTest : FunSpec({
    test("pause etter kort enhet bruker bare hundre millisekunder når rundebudsjettet nesten er brukt") {
        var nanos = 0L
        val ventinger = mutableListOf<Duration>()
        val struping = RyddeStruping(
            TilstandsRyddeKonfigurasjon(maksKjoretid = Duration.ofSeconds(10)),
            { nanos },
            { ventinger += it; nanos += it.toNanos() },
        )
        nanos = Duration.ofMillis(9900).toNanos()
        struping.etterEnhet(Duration.ofMillis(5))
        ventinger shouldBe listOf(Duration.ofMillis(100))
        struping.varighet() shouldBe Duration.ofSeconds(10)
        shouldThrow<RyddeStopp> { struping.kontroller() }.grunn shouldBe RyddeStoppgrunn.BUDSJETT
    }
    test("konfigurasjon avviser utrygge grenser") {
        listOf<() -> Unit>(
            { TilstandsRyddeKonfigurasjon(oppbevaringMaaneder = 5) },
            { TilstandsRyddeKonfigurasjon(intervallBredde = 10001) },
            { TilstandsRyddeKonfigurasjon(batchStorrelse = 101) },
            { TilstandsRyddeKonfigurasjon(maksReferanserPerBatch = Int.MAX_VALUE) },
            { TilstandsRyddeKonfigurasjon(pauseMin = Duration.ZERO) },
            { TilstandsRyddeKonfigurasjon(pauseMaks = Duration.ofMillis(1)) },
            { TilstandsRyddeKonfigurasjon(terskelEnhetstid = Duration.ZERO) },
            { TilstandsRyddeKonfigurasjon(maksKjoretid = Duration.ZERO) },
            { TilstandsRyddeKonfigurasjon(maksForsok = 0) },
            { TilstandsRyddeKonfigurasjon(sporringsTimeout = Duration.ofSeconds(Int.MAX_VALUE.toLong() + 1)) },
        ).forEach { shouldThrow<IllegalArgumentException>(it) }
    }
    test("første langsomme enhet dobler og raske enheter halverer pausen") {
        val ventinger = mutableListOf<Duration>()
        val struping = RyddeStruping(TilstandsRyddeKonfigurasjon(), { 0L }, { ventinger += it })
        struping.etterEnhet(Duration.ofSeconds(2))
        struping.pause shouldBe Duration.ofSeconds(2)
        struping.etterEnhet(Duration.ZERO)
        struping.pause shouldBe Duration.ofSeconds(1)
        struping.etterEnhet(Duration.ZERO, lasefeil = true)
        struping.pause shouldBe Duration.ofSeconds(30)
        ventinger shouldBe listOf(Duration.ofSeconds(2), Duration.ofSeconds(1), Duration.ofSeconds(30))
    }
    test("spørringstimeout er avgrenset av gjenstående monotont budsjett") {
        var nanos = 0L
        val struping = RyddeStruping(
            TilstandsRyddeKonfigurasjon(maksKjoretid = Duration.ofSeconds(6)),
            { nanos },
            {},
        )
        struping.timeoutSekunder() shouldBe 5
        nanos = Duration.ofSeconds(5).toNanos()
        struping.timeoutSekunder() shouldBe 1
        nanos += 1
        shouldThrow<RyddeStopp> { struping.timeoutSekunder() }.grunn shouldBe RyddeStoppgrunn.BUDSJETT
    }
    test("venting er begrenset av budsjett og stoppindikator kontrolleres") {
        var nanos = 0L
        var stopp = false
        val ventinger = mutableListOf<Duration>()
        val struping = RyddeStruping(
            TilstandsRyddeKonfigurasjon(maksKjoretid = Duration.ofSeconds(2)),
            { nanos },
            { ventinger += it; nanos += it.toNanos() },
            { stopp },
        )
        struping.etterEnhet(Duration.ofSeconds(2))
        ventinger shouldBe listOf(Duration.ofSeconds(2))
        shouldThrow<RyddeStopp> { struping.timeoutSekunder() }.grunn shouldBe RyddeStoppgrunn.BUDSJETT
        stopp = true
        shouldThrow<RyddeStopp> { struping.kontroller() }.grunn shouldBe RyddeStoppgrunn.AVBRUTT
    }
})
