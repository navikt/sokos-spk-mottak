package no.nav.sokos.spk.mottak.domain

import jakarta.xml.bind.annotation.XmlAccessType
import jakarta.xml.bind.annotation.XmlAccessorType
import jakarta.xml.bind.annotation.XmlAttribute
import jakarta.xml.bind.annotation.XmlElement
import jakarta.xml.bind.annotation.XmlRootElement

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
