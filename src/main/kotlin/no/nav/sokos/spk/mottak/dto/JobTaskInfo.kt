package no.nav.sokos.spk.mottak.dto

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class JobTaskInfo(
    val taskId: String,
    val taskName: String,
    val executionTime: Instant,
    val isPicked: Boolean,
    val pickedBy: String?,
    val lastFailure: Instant?,
    val lastSuccess: Instant?,
    val ident: String?,
)
