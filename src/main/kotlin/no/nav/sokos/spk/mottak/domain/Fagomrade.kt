package no.nav.sokos.spk.mottak.domain

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement

@JacksonXmlRootElement(localName = "Fagomrade")
data class Fagomrade(
    var trekkgruppeKode: String,
    var fagomradeKode: String? = null,
    var fagomradeBeskrivelse: String? = null,
    var gyldig: Boolean? = null,
    var endringsInfo: EndringsInfo? = null,
)
