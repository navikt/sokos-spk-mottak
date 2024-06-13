package no.nav.sokos.spk.mottak.domain

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.time.LocalDate

@JacksonXmlRootElement(localName = "EndringsInfo")
data class EndringsInfo(
    var endretAvId: String? = null,
    var endretAvNavn: String? = null,
    var endretAvEnhetId: String? = null,
    var endretAvEnhetNavn: String? = null,
    var opprettetAvId: String,
    var opprettetAvEnhetId: String? = null,
    var opprettetAvEnhetNavn: String? = null,
    var opprettetAvNavn: String? = null,
    var endretDato: LocalDate? = null,
    var opprettetDato: LocalDate? = null,
    var kildeId: String,
    var kildeNavn: String? = null,
)
