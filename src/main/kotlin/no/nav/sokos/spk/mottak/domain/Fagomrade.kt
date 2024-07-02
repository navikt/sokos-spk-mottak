package no.nav.sokos.spk.mottak.domain

import jakarta.xml.bind.annotation.XmlElement

data class Fagomrade(
    @XmlElement
    var trekkgruppeKode: String,
    @XmlElement
    var fagomradeKode: String? = null,
    @XmlElement
    var fagomradeBeskrivelse: String? = null,
    @XmlElement
    var gyldig: Boolean? = null,
    @XmlElement
    var endringsInfo: EndringsInfo? = null,
)
