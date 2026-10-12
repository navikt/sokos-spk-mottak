package no.nav.sokos.spk.mottak.domain

import java.time.Duration

enum class RyddeStoppgrunn(val utfall: String) {
    FULLFORT("fullfort"),
    BUDSJETT("budsjett"),
    AVBRUTT("avbrutt"),
    OPPTATT("opptatt"),
    FEIL("feil"),
}

class RyddeStopp(val grunn: RyddeStoppgrunn) : RuntimeException("Rydding stoppet: ${grunn.utfall}")

data class TilstandsRyddeOppsummering(
    val kandidater: Long,
    val slettede: Long,
    val nullstilteReferanser: Long,
    val varighet: Duration,
    val stoppgrunn: RyddeStoppgrunn,
)
