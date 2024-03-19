package no.nav.sokos.spk.mottak.domain.record

import java.time.LocalDate

data class StartRecord(
    val avsender: String,
    val mottager: String,
    val filLopenummer: Int,
    val filType: String,
    val produsertDato: LocalDate,
    val beskrivelse: String,
    var filStatus: String? = null,
    var feilTekst: String? = null
)