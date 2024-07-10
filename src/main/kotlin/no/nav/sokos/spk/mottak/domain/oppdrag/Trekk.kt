package no.nav.sokos.spk.mottak.domain.oppdrag

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

@XmlRootElement(name = "dokument")
@XmlAccessorType(XmlAccessType.FIELD)
data class Dokument(
    @field:XmlElement(name = "innrapporteringTrekk")
    val innrapporteringTrekk: InnrapporteringTrekk? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class InnrapporteringTrekk(
    @field:XmlElement(name = "aksjonskode")
    val aksjonskode: String,
    @field:XmlElement(name = "navTrekkId")
    val navTrekkId: String? = null,
    @field:XmlElement(name = "kreditorIdTss")
    val kreditorIdTss: String,
    @field:XmlElement(name = "kreditorTrekkId")
    val kreditorTrekkId: String,
    @field:XmlElement(name = "kreditorOrgnr")
    val kreditorOrgnr: String? = null,
    @field:XmlElement(name = "kontonr")
    val kontonr: String? = null,
    @field:XmlElement(name = "debitorId")
    val debitorId: String,
    @field:XmlElement(name = "kodeTrekktype")
    val kodeTrekktype: String,
    @field:XmlElement(name = "kodeTrekkAlternativ")
    val kodeTrekkAlternativ: String,
    @field:XmlElement(name = "kid")
    val kid: String? = null,
    @field:XmlElement(name = "kreditorsRef")
    val kreditorsRef: String? = null,
    @field:XmlElement(name = "saldo")
    val saldo: Double? = null,
    @field:XmlElement(name = "prioritetFomDato")
    val prioritetFomDato: String? = null,
    @field:XmlElement(name = "gyldigTomDato")
    val gyldigTomDato: String? = null,
    @field:XmlElement(name = "periode")
    val periode: Periode,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Periode(
    @field:XmlElement(name = "periodeFomDato")
    val periodeFomDato: String,
    @field:XmlElement(name = "periodeTomDato")
    val periodeTomDato: String,
    @field:XmlElement(name = "sats")
    val sats: Double,
)
