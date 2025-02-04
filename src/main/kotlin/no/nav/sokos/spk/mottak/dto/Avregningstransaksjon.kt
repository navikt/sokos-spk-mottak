package no.nav.sokos.spk.mottak.dto

import java.time.LocalDate

data class Avregningstransaksjon(
    val transaksjonId: Int,
    val fnr: String,
    val transEksId: String,
    val datoAnviser: LocalDate,
)
