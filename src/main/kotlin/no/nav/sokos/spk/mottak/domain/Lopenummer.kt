package no.nav.sokos.spk.mottak.domain

import java.time.LocalDateTime

data class Lopenummer(
    val lopenummerId: Int,
    val sisteLopenummer: Int,
    val filType: String,
    val anviser: String,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val endretAv: String,
    val versjon: Int
)
