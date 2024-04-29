package no.nav.sokos.spk.mottak.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class AvvikTransaksjon(
    val avvikTransaksjonId: Int? = null,
    val filInfoId: Int,
    val transaksjonStatus: String,
    val fnr: String,
    val belopType: String,
    val art: String,
    val avsender: String,
    val utbetalesTil: String,
    val datoFom: String,
    val datoTom: String,
    val datoAnviser: LocalDate,
    val belop: String,
    val refTransId: String,
    val tekstKode: String,
    val rectType: String,
    val transEksId: String,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val endretAv: String,
    val versjon: Int,
    val prioritet: String,
    val saldo: String,
    val trekkansvar: String,
    val kid: String,
    val grad: Int?
)