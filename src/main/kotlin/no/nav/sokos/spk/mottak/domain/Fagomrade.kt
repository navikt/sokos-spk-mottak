package no.nav.sokos.spk.mottak.domain

data class Fagomrade(
    var trekkgruppeKode: String,
    var fagomradeKode: String? = null,
    var fagomradeBeskrivelse: String? = null,
    var gyldig: Boolean? = null,
    var endringsInfo: EndringsInfo? = null,
)
