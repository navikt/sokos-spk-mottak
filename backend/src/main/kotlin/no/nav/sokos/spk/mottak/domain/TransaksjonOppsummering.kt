package no.nav.sokos.spk.mottak.domain

import java.math.BigDecimal

data class TransaksjonOppsummering(
    val personId: Int,
    val fagomrade: String,
    val filInfoId: Int,
    val osStatus: Int?,
    val transTilstandType: String,
    val antall: Int,
    val belop: BigDecimal,
)
