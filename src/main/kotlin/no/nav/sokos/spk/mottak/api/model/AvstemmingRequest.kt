package no.nav.sokos.spk.mottak.api.model

import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class AvstemmingRequest(val fromDate: LocalDate?, val toDate: LocalDate?)
