package no.nav.sokos.spk.mottak.domain

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.Serializable

@Serializable
data class ScheduledTask(
    val id: String,
    val ident: String,
    val timestamp: LocalDateTime,
    val taskName: String,
)
