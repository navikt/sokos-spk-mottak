package no.nav.sokos.spk.mottak.domain

import java.math.BigInteger

data class AvstemmingData(
    val fagomrade: String,
    val transTilstandType: String,
    val antall: Int,
    val belop: BigInteger,
)
