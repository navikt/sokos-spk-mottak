package no.nav.sokos.spk.mottak.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class FilInfo(
    val filInfoId: Int? = null,
    val filStatus: String,
    val anviser: String,
    val filType: String,
    val filTilstandType: String,
    val filNavn: String,
    val lopenr: Int,
    val feiltekst: String?,
    val datoMottatt: LocalDate,
    val datoSendt: LocalDate?,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val endretAv: String,
    val versjon: Int
)

