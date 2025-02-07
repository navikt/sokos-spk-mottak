package no.nav.sokos.spk.mottak.dto

import java.time.LocalDate

data class Avregningstransaksjon(
    val transaksjonId: Int? = 0,
    val fnr: String? = null,
    val transEksId: String? = null,
    val datoAnviser: LocalDate? = null,
)
