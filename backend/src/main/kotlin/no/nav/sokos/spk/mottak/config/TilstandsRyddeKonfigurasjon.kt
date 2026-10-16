package no.nav.sokos.spk.mottak.config

import java.time.Duration

data class TilstandsRyddeKonfigurasjon(
    val oppbevaringMaaneder: Int = 6,
    val intervallBredde: Int = 1000,
    val batchStorrelse: Int = 100,
    val maksReferanserPerBatch: Int = 200,
    val pauseMin: Duration = Duration.ofSeconds(1),
    val pauseMaks: Duration = Duration.ofSeconds(30),
    val terskelEnhetstid: Duration = Duration.ofSeconds(1),
    val maksKjoretid: Duration = Duration.ofMinutes(15),
    val maksForsok: Int = 3,
    val sporringsTimeout: Duration = Duration.ofSeconds(5),
) {
    init {
        require(oppbevaringMaaneder >= 6)
        require(intervallBredde in 1..10000)
        require(batchStorrelse in 1..100)
        require(maksReferanserPerBatch in 1 until Int.MAX_VALUE)
        listOf(pauseMin, pauseMaks, terskelEnhetstid, maksKjoretid, sporringsTimeout).forEach {
            require(it > Duration.ZERO && it <= Duration.ofNanos(Long.MAX_VALUE)) { "Varighet må være positiv og nanosikker" }
        }
        require(pauseMaks >= pauseMin)
        require(maksForsok > 0)
        require(sporringsTimeout <= Duration.ofSeconds(Int.MAX_VALUE.toLong()))
    }
}
