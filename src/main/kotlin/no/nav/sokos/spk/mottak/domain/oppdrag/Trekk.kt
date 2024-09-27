package no.nav.sokos.spk.mottak.domain.oppdrag

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlElementWrapper
import jakarta.xml.bind.annotation.XmlRootElement
import kotlinx.serialization.Serializable

@Serializable
@XmlRootElement(name = "dokument")
@XmlAccessorType(XmlAccessType.FIELD)
data class Dokument(
    @XmlElement(name = "mmel")
    val mmel: Mmel? = null,
    @XmlElement(name = "transaksjonsId")
    val transaksjonsId: String? = null,
    @XmlElement(name = "innrapporteringTrekk")
    val innrapporteringTrekk: InnrapporteringTrekk? = null,
)

@Serializable
@XmlAccessorType(XmlAccessType.FIELD)
data class InnrapporteringTrekk(
    @XmlElement(name = "aksjonskode")
    val aksjonskode: String = "",
    @XmlElement(name = "navTrekkId")
    val navTrekkId: String? = null,
    @XmlElement(name = "kreditorIdTss")
    val kreditorIdTss: String = "",
    @XmlElement(name = "kreditorTrekkId")
    val kreditorTrekkId: String = "",
    @XmlElement(name = "debitorId")
    val debitorId: String = "",
    @XmlElement(name = "kodeTrekktype")
    val kodeTrekktype: String = "",
    @XmlElement(name = "kodeTrekkAlternativ")
    val kodeTrekkAlternativ: String = "",
    @XmlElement(name = "kid")
    val kid: String? = null,
    @XmlElement(name = "kreditorsRef")
    val kreditorsRef: String? = null,
    @XmlElement(name = "saldo")
    val saldo: Double? = null,
    @XmlElement(name = "prioritetFomDato")
    val prioritetFomDato: String? = null,
    @XmlElement(name = "gyldigTomDato")
    val gyldigTomDato: String? = null,
    @XmlElementWrapper(name = "perioder")
    @XmlElement(name = "periode")
    val periode: MutableList<Periode> = mutableListOf(),
)

@Serializable
@XmlAccessorType(XmlAccessType.FIELD)
data class Periode(
    @XmlElement(name = "periodeFomDato")
    val periodeFomDato: String = "",
    @XmlElement(name = "periodeTomDato")
    val periodeTomDato: String = "",
    @XmlElement(name = "sats")
    val sats: Double = 0.0,
)

@Serializable
@XmlAccessorType(XmlAccessType.FIELD)
data class Mmel(
    @XmlElement(name = "systemId")
    val systemId: String? = null,
    @XmlElement(name = "kodeMelding")
    val kodeMelding: String? = null,
    @XmlElement(name = "alvorlighetsgrad")
    val alvorlighetsgrad: String? = null,
    @XmlElement(name = "beskrMelding")
    val beskrMelding: String? = null,
    @XmlElement(name = "sqlKode")
    val sqlKode: String? = null,
    @XmlElement(name = "sqlStateMmel")
    val sqlStateMmel: String? = null,
    @XmlElement(name = "sqlMelding")
    val sqlMelding: String? = null,
    @XmlElement(name = "mqCompletionKode")
    val mqCompletionKode: String? = null,
    @XmlElement(name = "mqReasonKode")
    val mqReasonKode: String? = null,
    @XmlElement(name = "programId")
    val programId: String? = null,
    @XmlElement(name = "sectionNavn")
    val sectionNavn: String? = null,
)
