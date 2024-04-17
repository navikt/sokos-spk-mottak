package no.nav.sokos.spk.mottak.domain

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.sokos.spk.mottak.config.PropertiesConfig

data class InnTransaksjon(
    val innTransaksjonId: Int? = null,
    val filInfoId: Int,
    val transaksjonStatus: String?,
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
    val rectype: String,
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
    val gradStr: String,
    val personId: Int? = null
)

fun InnTransaksjon.toTransaksjon(datoPersonFom: LocalDate, datoLeakFom: LocalDate, transTolkning: String, fnrEndret: Boolean, motId: String): Transaksjon {
    val systemId = PropertiesConfig.Configuration().naisAppName
    return Transaksjon(
        filInfoId = this.filInfoId,
        transaksjonStatus = this.transaksjonStatus!!,
        personId = this.personId!!,
        beloptype = this.belopstype,
        art = this.art,
        anviser = this.avsender,
        fnr = this.fnr,
        utbetalesTil = this.utbetalesTil,
        datoFom = this.datoFom,
        datoTom = this.datoTom,
        datoAnviser = this.datoAnviser,
        datoPersonFom = datoPersonFom,
        datoLeakFom = datoLeakFom,
        belop = this.belop,
        refTransId = this.refTransId,
        tekstkode = this.tekstKode,
        rectype = this.rectype,
        transEksId = this.transId,
        transTolkning = transTolkning,
        sendtTilOppdrag = "0",
        fnrEndret = fnrEndret,
        motId = motId,
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = systemId,
        datoEndret = LocalDateTime.now(),
        endretAv = systemId,
        versjon = 1,
        saldo = this.saldo,
        kid = this.kid,
        prioritet = this.prioritet,
        trekkansvar = TREKKANSVAR_4819,
        transTilstandType = TRANS_TILSTAND_OPR,
        grad = this.grad
    )
}


fun InnTransaksjon.isTransaksjonStatusOK(): Boolean = this.transaksjonStatus == TRANSAKSJONSTATUS_OK