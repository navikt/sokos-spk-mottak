package no.nav.sokos.spk.mottak.domain

import java.time.LocalDate
import java.time.LocalDateTime

data class Transaksjon(
    val transaksjonId: Int? = null,
    val filInfoId: Int,
    val transaksjonStatus: String,
    val personId: Int,
    val belopstype: String,
    val art: String,
    val anviser: String,
    val fnr: String,
    val utbetalesTil: String?,
    val osId: String? = null,
    val osLinjeId: String? = null,
    val datoFom: LocalDate?,
    val datoTom: LocalDate?,
    val datoAnviser: LocalDate?,
    val datoPersonFom: LocalDate,
    val datoReakFom: LocalDate? = null,
    val belop: Int,
    val refTransId: String?,
    val tekstkode: String?,
    val rectype: String,
    val transEksId: String,
    val transTolkning: String,
    val sendtTilOppdrag: String,
    val trekkvedtakId: String? = null,
    val fnrEndret: Char,
    val motId: String,
    val osStatus: String? = null,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val endretAv: String,
    val versjon: Int,
    val transTilstandType: String? = null,
    val grad: Int?,
    val trekkType: String? = null,
    val trekkAlternativ: String? = null,
    val trekkGruppe: String? = null,
    val gyldigKombinasjon: GyldigKombinasjon? = null,
)

fun Transaksjon.getTransTolkningOppdragKode(): String {
    return when (transTolkning) {
        TRANS_TOLKNING_NY -> "NY"
        TRANS_TOLKNING_NY_EKSIST -> "ENDR"
        else -> "UEND"
    }
}
