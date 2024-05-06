package no.nav.sokos.spk.mottak.integration.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FullmaktDTO(
    @SerialName("aktorIdentGirFullmakt")
    val aktorIdentGirFullmakt: String,
    @SerialName("aktorIdentMottarFullmakt")
    val aktorIdentMottarFullmakt: String,
    @SerialName("kodeAktorTypeGirFullmakt")
    val kodeAktorTypeGirFullmakt: String,
    @SerialName("kodeAktorTypeMottarFullmakt")
    val kodeAktorTypeMottarFullmakt: String,
    @SerialName("kodeFullmaktType")
    val kodeFullmaktType: String,
)
