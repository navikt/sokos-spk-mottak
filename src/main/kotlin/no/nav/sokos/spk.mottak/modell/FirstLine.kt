package no.nav.sokos.spk.mottak.modell

import java.time.LocalDate

class FirstLine(
    val avsender: String,
    val mottager: String,
    val filLopenummer: Int,
    val filType: String,
    val produsertDato: LocalDate,
    val beskrivelse: String,
    var filStatus: String,
    var feilTekst: String
) {
    override fun toString(): String {
        return "FirstLine(avsender='$avsender', mottager='$mottager', filLopenummer=$filLopenummer, filType='$filType', produsertDato=$produsertDato, beskrivelse='$beskrivelse', filStatus='$filStatus', feilTekst='$feilTekst')"
    }
}