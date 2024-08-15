package no.nav.sokos.spk.mottak.domain.oppdrag

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import kotlinx.serialization.Serializable

@Serializable
@XmlRootElement(name = "dokument")
@XmlAccessorType(XmlAccessType.FIELD)
data class Dokument(
    @XmlElement(name = "innrapporteringTrekk")
    val innrapporteringTrekk: InnrapporteringTrekk? = null,
)

@Serializable
@XmlAccessorType(XmlAccessType.FIELD)
data class InnrapporteringTrekk(
    @XmlElement(name = "aksjonskode")
    val aksjonskode: String,
    @XmlElement(name = "navTrekkId")
    val navTrekkId: String? = null,
    @XmlElement(name = "kreditorIdTss")
    val kreditorIdTss: String,
    @XmlElement(name = "kreditorTrekkId")
    val kreditorTrekkId: String,
    @XmlElement(name = "kreditorOrgnr")
    val kreditorOrgnr: String? = null,
    @XmlElement(name = "kontonr")
    val kontonr: String? = null,
    @XmlElement(name = "debitorId")
    val debitorId: String,
    @XmlElement(name = "kodeTrekktype")
    val kodeTrekktype: String,
    @XmlElement(name = "kodeTrekkAlternativ")
    val kodeTrekkAlternativ: String,
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
    @XmlElement(name = "periode")
    val periode: Periode,
)

@Serializable
@XmlAccessorType(XmlAccessType.FIELD)
data class Periode(
    @XmlElement(name = "periodeFomDato")
    val periodeFomDato: String,
    @XmlElement(name = "periodeTomDato")
    val periodeTomDato: String,
    @XmlElement(name = "sats")
    val sats: Double,
)
