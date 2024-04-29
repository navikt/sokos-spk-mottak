package no.nav.sokos.spk.mottak.domain

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.util.Util.toChar

data class InnTransaksjon(
    val innTransaksjonId: Int? = null,
    val filInfoId: Int,
    val transaksjonStatus: String?,
    val fnr: String,
    val belopstype: String,
    val art: String,
    val avsender: String,
    val utbetalesTil: String?,
    val datoFomStr: String,
    val datoTomStr: String,
    val datoAnviserStr: String,
    val belopStr: String,
    val refTransId: String?,
    val tekstkode: String?,
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
    val grad: Int?,
    val gradStr: String?,
    val personId: Int? = null
)

fun InnTransaksjon.toTransaksjon(transaksjon: Transaksjon?): Transaksjon {
    val systemId = PropertiesConfig.Configuration().naisAppName
    return Transaksjon(
        transaksjonId = this.innTransaksjonId,
        filInfoId = this.filInfoId,
        transaksjonStatus = this.transaksjonStatus!!,
        personId = this.personId ?: 600002,
        belopstype = this.belopstype,
        art = this.art,
        anviser = this.avsender,
        fnr = this.fnr,
        utbetalesTil = this.utbetalesTil,
        datoFom = this.datoFom,
        datoTom = this.datoTom,
        datoAnviser = this.datoAnviser,
        datoPersonFom = LocalDate.of(1900, 1, 1),
        datoReakFom = LocalDate.of(1900, 1, 1),
        belop = this.belop,
        refTransId = this.refTransId,
        tekstkode = this.tekstkode,
        rectype = this.rectype,
        transEksId = this.transId,
        transTolkning = transaksjon?.let { TRANS_TOLKNING_NY_EKSIST } ?: TRANS_TOLKNING_NY,
        sendtTilOppdrag = "0",
        fnrEndret = (transaksjon?.let { it.fnr != this.fnr } ?: false).toChar(),
        motId = null,
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = systemId,
        datoEndret = LocalDateTime.now(),
        endretAv = systemId,
        versjon = 1,
        transTilstandType = TRANS_TILSTAND_OPR,
        grad = this.grad
    )
}


fun InnTransaksjon.isTransaksjonStatusOK(): Boolean = this.transaksjonStatus == TRANSAKSJONSTATUS_OK