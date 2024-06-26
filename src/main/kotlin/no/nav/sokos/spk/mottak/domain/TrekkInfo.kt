package no.nav.sokos.spk.mottak.domain

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement

@XmlAccessorType(XmlAccessType.FIELD)
data class TrekkInfo(
    @field:XmlElement(name = "Aksjonskode")
    var aksjonskode: Aksjonskode? = null,
    @field:XmlElement(name = "Identifisering")
    var identifisering: Identifisering? = null,
    @field:XmlElement(name = "Trekk")
    var trekk: Trekk? = null,
    @field:XmlElement(name = "Periode")
    var periode: Periode? = null,
    @field:XmlElement(name = "Kreditor")
    var kreditor: Kreditor? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Aksjonskode(
    @field:XmlAttribute(name = "V")
    var v: String? = "NY",
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Identifisering(
    // transaksjon.transEksId
    @field:XmlElement(name = "KreditorTrekkId")
    var kreditorTrekkId: String? = null,
    @field:XmlElement(name = "DebitorId")
    var debitorId: DebitorId? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class DebitorId(
    // transaksjon.fnr
    @field:XmlElement(name = "Id")
    var id: String? = null,
    @field:XmlElement(name = "TypeId")
    var typeId: TypeId? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class TypeId(
    @field:XmlAttribute(name = "V")
    var v: String? = "FNR",
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Trekk(
    @field:XmlElement(name = "KodeTrekktype")
    var kodeTrekktype: KodeTrekktype? = null,
    @field:XmlElement(name = "KodeTrekkAlternativ")
    var kodeTrekkAlternativ: KodeTrekkAlternativ? = null,
    @field:XmlElement(name = "Sats")
    var sats: Sats? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class KodeTrekktype(
    // transaksjon.trekkType (SPK1 eller SPK2)
    @field:XmlAttribute(name = "V")
    var v: String? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class KodeTrekkAlternativ(
    // transaksjon.trekkAlternativ (LOPM)
    @field:XmlAttribute(name = "V")
    var v: String? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Sats(
    // transaksjon.belop / 100.0
    @field:XmlAttribute(name = "V")
    var v: String? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Periode(
    // transaksjon.datoFom
    @field:XmlElement(name = "PeriodeFomDato")
    var periodeFomDato: String? = null,
    // transaksjon.datoTom
    @field:XmlElement(name = "PeriodeTomDato")
    var periodeTomDato: String? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Kreditor(
    // transaksjon.transEksId
    @field:XmlElement(name = "TssId", namespace = "ivt")
    var tssId: String? = null,
)
