package no.nav.sokos.spk.mottak.domain

import kotlinx.serialization.Serializable

@Serializable
data class LeveAttest(
    val fnrFk: String,
    val kAnviser: String,
)
