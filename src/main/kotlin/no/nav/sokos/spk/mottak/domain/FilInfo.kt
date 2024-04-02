package no.nav.sokos.spk.mottak.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class FilInfo(
    var id: Int = -1,
    val status: String,
    val tilstand: String,
    val anviser: String,
    val filnavn: String,
    val lopenr: Int,
    val datoMottatt: LocalDate,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val endretAv: String,
    val versjon: Int,
    val filType: String,
    val feilTekst: String?
)

