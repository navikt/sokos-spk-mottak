package no.nav.sokos.spk.mottak.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class InnTransaksjon(
    val innTransaksjonId: Int? = null,
    val filInfoId: Int,
    val transaksjonStatus: String,
    val fnr: String,
    val belopstype: String,
    val art: String,
    val avsender: String,
    val utbetalesTil: String,
    val datoFomStr: String,
    val datoTomStr: String,
    val datoAnviserStr: String,
    val belopStr: String,
    val refTransId: String,
    val tekstKode: String,
    val recType: String,
    val transId: String,
    val datoFom: LocalDate,
    val datoTom: LocalDate,
    val datoAnviser: LocalDate,
    val belop: Int,
    val behandlet: String,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val endretAv: String,
    val versjon: Int,
    val prioritetStr: String,
    val trekkansvar: String,
    val saldoStr: String,
    val kid: String,
    val prioritet: LocalDate?,
    val saldo: Int,
    val grad: Int?,
    val gradStr: String
)