package no.nav.sokos.spk.mottak.domain.converter

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement
import no.nav.sokos.spk.mottak.domain.SPK_TSS
import no.nav.sokos.spk.mottak.domain.Transaksjon
import java.time.format.DateTimeFormatter

private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

object TrekkConverter {
    fun opprettTrekkMelding(it: Transaksjon) =
        TrekkMelding(
            msgInfo =
                MsgInfo(
                    type = Type(),
                    sender =
                        Sender(
                            organisation = Organisation(),
                        ),
                    receiver =
                        Receiver(
                            organisation = Organisation(),
                        ),
                ),
            document =
                Document(
                    refDoc =
                        RefDoc(
                            msgType = MsgType(),
                            content =
                                Content(
                                    innrapporteringTrekk = opprettTrekk(it),
                                ),
                        ),
                ),
        )

    private fun opprettTrekk(transaksjon: Transaksjon) =
        TrekkInfo(
            aksjonskode = Aksjonskode(),
            identifisering =
                Identifisering(
                    kreditorTrekkId = transaksjon.transEksId,
                    debitorId =
                        DebitorId(
                            id = transaksjon.fnr,
                            typeId = TypeId(),
                        ),
                ),
            trekk =
                Trekk(
                    kodeTrekktype = KodeTrekktype(v = transaksjon.trekkType!!),
                    kodeTrekkAlternativ = KodeTrekkAlternativ(v = transaksjon.trekkAlternativ!!),
                    sats = Sats((transaksjon.belop / 100.0).toString()),
                ),
            periode =
                Periode(
                    periodeFomDato = transaksjon.datoFom!!.format(formatter),
                    periodeTomDato = transaksjon.datoTom!!.format(formatter),
                ),
            kreditor =
                Kreditor(
                    tssId = SPK_TSS,
                ),
        )
}

@XmlRootElement(name = "MsgHead")
@XmlAccessorType(XmlAccessType.FIELD)
data class TrekkMelding(
    @field:XmlElement(name = "MsgInfo")
    var msgInfo: MsgInfo? = null,
    @field:XmlElement(name = "Document")
    var document: Document? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class MsgInfo(
    @field:XmlElement(name = "Type")
    var type: Type? = null,
    @field:XmlElement(name = "Sender")
    var sender: Sender? = null,
    @field:XmlElement(name = "Receiver")
    var receiver: Receiver? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Type(
    @field:XmlAttribute(name = "V")
    var v: String? = "INNRAPPORTERING_TREKK",
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Sender(
    @field:XmlElement(name = "Organisation")
    var organisation: Organisation? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Receiver(
    @field:XmlElement(name = "Organisation")
    var organisation: Organisation? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Organisation(
    @field:XmlElement(name = "OrganisationName")
    var organisationName: String? = "NAV",
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Document(
    @field:XmlElement(name = "ContentDescription")
    var contentDescription: String? = "Innrapportering av trekk",
    @field:XmlElement(name = "RefDoc")
    var refDoc: RefDoc? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class RefDoc(
    @field:XmlElement(name = "MsgType")
    var msgType: MsgType? = null,
    @field:XmlElement(name = "Content")
    var content: Content? = null,
)

@XmlAccessorType(XmlAccessType.FIELD)
data class MsgType(
    @field:XmlAttribute(name = "V")
    var v: String? = "XML",
)

@XmlAccessorType(XmlAccessType.FIELD)
data class Content(
    @field:XmlElement(name = "InnrapporteringTrekk")
    var innrapporteringTrekk: TrekkInfo? = null,
)

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
