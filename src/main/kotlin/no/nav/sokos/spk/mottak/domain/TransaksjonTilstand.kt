package no.nav.sokos.spk.mottak.domain

import java.time.LocalDateTime

data class TransaksjonTilstand(
    val transaksjonTilstandId: Int? = null,
    val transaksjonId: Int,
    val transaksjonTilstandType: String,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val endretAv: String,
    val versjon: Int
)