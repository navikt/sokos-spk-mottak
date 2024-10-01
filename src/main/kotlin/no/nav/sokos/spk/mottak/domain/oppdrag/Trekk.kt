package no.nav.sokos.spk.mottak.domain.oppdrag

import kotlinx.serialization.Serializable

@Serializable
data class DokumentWrapper(
    val dokument: Dokument
)

@Serializable
data class Dokument(
    val mmel: Mmel? = null,
    val transaksjonsId: String? = null,
    val innrapporteringTrekk: InnrapporteringTrekk? = null,
)

@Serializable
data class InnrapporteringTrekk(
    val aksjonskode: String = "",
    val navTrekkId: String? = null,
    val kreditorIdTss: String = "",
    val kreditorTrekkId: String = "",
    val debitorId: String = "",
    val kodeTrekktype: String = "",
    val kodeTrekkAlternativ: String = "",
    val kid: String? = null,
    val kreditorsRef: String? = null,
    val saldo: Double? = null,
    val prioritetFomDato: String? = null,
    val gyldigTomDato: String? = null,
    val periode: MutableList<Periode> = mutableListOf(),
)

@Serializable
data class Periode(
    val periodeFomDato: String = "",
    val periodeTomDato: String = "",
    val sats: Double = 0.0,
)

@Serializable
data class Mmel(
    val systemId: String? = null,
    val kodeMelding: String? = null,
    val alvorlighetsgrad: String? = null,
    val beskrMelding: String? = null,
    val sqlKode: String? = null,
    val sqlStateMmel: String? = null,
    val sqlMelding: String? = null,
    val mqCompletionKode: String? = null,
    val mqReasonKode: String? = null,
    val programId: String? = null,
    val sectionNavn: String? = null,
)
