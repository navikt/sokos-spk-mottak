package no.nav.sokos.spk.mottak.domain

import java.time.LocalDateTime

data class Person(
    val personId: Int? = null,
    val fnr: String,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val endretAv: String,
    val versjon: Int,
)
