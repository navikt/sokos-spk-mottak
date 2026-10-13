package no.nav.sokos.spk.mottak.service

import java.time.Duration

import no.nav.sokos.spk.mottak.config.TilstandsRyddeKonfigurasjon
import no.nav.sokos.spk.mottak.domain.RyddeStopp
import no.nav.sokos.spk.mottak.domain.RyddeStoppgrunn

/**
 * Begrenser belastningen fra tilstandsrydding med adaptive pauser mellom arbeidsenheter.
 * Pausen dobles ved trege enheter, settes til maksimum ved låsefeil og halveres ved
 * raske enheter, innenfor konfigurerte grenser. Venting skal skje uten åpne transaksjoner.
 *
 * Bruker monoton tid til å kontrollere kjørebudsjettet og begrenser SQL-timeout til
 * gjenstående hele sekunder. Avbrudd, stoppsignal eller oppbrukt budsjett gir [RyddeStopp].
 * Budsjettet hindrer nytt arbeid, men avbryter ikke en pågående databaseoperasjon.
 */
class RyddeStruping(
    private val konfigurasjon: TilstandsRyddeKonfigurasjon,
    private val nanoTime: () -> Long = System::nanoTime,
    private val vent: (Duration) -> Unit = { Thread.sleep(it) },
    private val stoppindikator: () -> Boolean = { false },
) {
    private val start = nanoTime()
    var pause: Duration = konfigurasjon.pauseMin
        private set

    fun varighet(): Duration = Duration.ofNanos((nanoTime() - start).coerceAtLeast(0))

    fun kontroller() {
        if (Thread.currentThread().isInterrupted || stoppindikator()) throw RyddeStopp(RyddeStoppgrunn.AVBRUTT)
        if (varighet() >= konfigurasjon.maksKjoretid) throw RyddeStopp(RyddeStoppgrunn.BUDSJETT)
    }

    fun timeoutSekunder(): Int {
        kontroller()
        val gjenstaende = (konfigurasjon.maksKjoretid - varighet()).seconds
        if (gjenstaende < 1) throw RyddeStopp(RyddeStoppgrunn.BUDSJETT)
        val timeout = konfigurasjon.sporringsTimeout
        val avrundet = timeout.seconds + if (timeout.nano == 0) 0 else 1
        return minOf(avrundet, gjenstaende).toInt()
    }

    fun etterEnhet(
        varighet: Duration,
        lasefeil: Boolean = false,
    ) {
        pause =
            when {
                lasefeil -> konfigurasjon.pauseMaks
                varighet > konfigurasjon.terskelEnhetstid ->
                    if (pause >= konfigurasjon.pauseMaks.dividedBy(2)) {
                        konfigurasjon.pauseMaks
                    } else {
                        pause.multipliedBy(2).coerceAtMost(konfigurasjon.pauseMaks)
                    }
                else -> pause.dividedBy(2).coerceAtLeast(konfigurasjon.pauseMin)
            }
        kontroller()
        try {
            vent(minOf(pause, konfigurasjon.maksKjoretid - varighet()))
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            throw RyddeStopp(RyddeStoppgrunn.AVBRUTT)
        }
    }
}
