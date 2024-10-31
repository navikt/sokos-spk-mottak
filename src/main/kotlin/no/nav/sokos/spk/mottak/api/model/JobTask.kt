package no.nav.sokos.spk.mottak.api.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class JobTask(
    val taskId: String,
    val taskName: String,
    val executionTime: Instant,
    val isPicked: Boolean,
    val pickedBy: String?,
    val lastFailure: Instant?,
    val lastSuccess: Instant?,
)
