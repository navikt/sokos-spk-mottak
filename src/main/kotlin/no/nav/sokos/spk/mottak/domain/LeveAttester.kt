package no.nav.sokos.spk.mottak.domain

import kotlinx.serialization.Serializable

@Serializable
data class LeveAttester(
    val fnrFk: String,
    val kAnviser: String,
)
