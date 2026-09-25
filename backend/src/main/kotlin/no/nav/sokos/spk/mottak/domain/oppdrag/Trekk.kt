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
    val alvorlighetsgrad: String,
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
    val transaksjonsId: String,
    val innrapporteringTrekk: InnrapporteringTrekk,
)

@Serializable
data class InnrapporteringTrekk(
    val aksjonskode: String,
    val kilde: String,
    val navTrekkId: String? = null,
    val kreditorIdTss: String,
    val kreditorTrekkId: String,
    val debitorId: String,
    val kodeTrekktype: String,
    val kodeTrekkAlternativ: String,
    val perioder: Perioder,
)

@Serializable
data class Perioder(
    val periode: MutableList<Periode> = mutableListOf(),
)

@Serializable
data class Periode(
    val periodeFomDato: String,
    val periodeTomDato: String,
    val sats: Double,
)
