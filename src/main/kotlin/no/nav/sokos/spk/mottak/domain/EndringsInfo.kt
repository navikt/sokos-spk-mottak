package no.nav.sokos.spk.mottak.domain

import jakarta.xml.bind.annotation.XmlElement
import java.time.LocalDate

data class EndringsInfo(
    @XmlElement
    var endretAvId: String? = null,
    @XmlElement
    var endretAvNavn: String? = null,
    @XmlElement
    var endretAvEnhetId: String? = null,
    @XmlElement
    var endretAvEnhetNavn: String? = null,
    @XmlElement
    var opprettetAvId: String,
    @XmlElement
    var opprettetAvEnhetId: String? = null,
    @XmlElement
    var opprettetAvEnhetNavn: String? = null,
    @XmlElement
    var opprettetAvNavn: String? = null,
    @XmlElement
    var endretDato: LocalDate? = null,
    @XmlElement
    var opprettetDato: LocalDate? = null,
    @XmlElement
    var kildeId: String,
    @XmlElement
    var kildeNavn: String? = null,
)
