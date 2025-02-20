package no.nav.sokos.spk.mottak.dto

import java.time.LocalDate

data class Avregningstransaksjon(
    val transaksjonId: Int? = null,
    val fnr: String? = null,
    val transEksId: String? = null,
    val datoAnviser: LocalDate? = null,
)
