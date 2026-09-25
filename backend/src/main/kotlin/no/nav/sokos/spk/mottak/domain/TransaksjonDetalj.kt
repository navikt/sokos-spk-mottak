package no.nav.sokos.spk.mottak.domain

import java.time.LocalDateTime

data class TransaksjonDetalj(
    val transaksjonId: Int,
    val fnr: String,
    val fagsystemId: String,
    val osStatus: String? = null,
    val transTilstandType: String,
    val feilkode: String? = null,
    val feilkodeMelding: String? = null,
    val datoOpprettet: LocalDateTime,
)
