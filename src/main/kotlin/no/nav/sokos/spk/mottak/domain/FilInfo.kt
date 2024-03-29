package no.nav.sokos.spk.mottak.domain

import java.time.LocalDateTime

data class FilInfo(
    var id: Int = -1,
    val status: String,
    val tilstand: String,
    val anviser: String,
    val filnavn: String,
    val lopenr: Int,
    val datoMottatt: String? = null,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val datoSendt: String? = null,
    val endretAv: String,
    val versjon: Int,
    val filType: String,
    val feilTekst: String?
)

