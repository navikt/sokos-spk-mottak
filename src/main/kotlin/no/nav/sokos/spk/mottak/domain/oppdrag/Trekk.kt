package no.nav.sokos.spk.mottak.domain.oppdrag

import kotlinx.serialization.Serializable

@Serializable
data class DokumentWrapper(
    val mmel: Mmel? = null,
    val dokument: Dokument? = null,
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

@Serializable
data class Dokument(
    val transaksjonsId: String? = null,
    val innrapporteringTrekk: InnrapporteringTrekk? = null,
)

@Serializable
data class InnrapporteringTrekk(
    val aksjonskode: String? = null,
    val navTrekkId: String? = null,
    val kreditorIdTss: String? = null,
    val kreditorTrekkId: String? = null,
    val debitorId: String? = null,
    val kodeTrekktype: String? = null,
    val kodeTrekkAlternativ: String? = null,
    val perioder: Perioder? = null,
)

@Serializable
data class Perioder(
    val periode: MutableList<Periode> = mutableListOf(),
)

@Serializable
data class Periode(
    val periodeFomDato: String? = null,
    val periodeTomDato: String? = null,
    val sats: Double = 0.0,
)
