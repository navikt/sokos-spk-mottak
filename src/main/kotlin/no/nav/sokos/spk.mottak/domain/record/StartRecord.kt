package no.nav.sokos.spk.mottak.domain.record

import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilTilstandType
import no.nav.sokos.spk.mottak.validator.FileStatusValidation
import java.time.LocalDate
import java.time.LocalDateTime

data class StartRecord(
    val avsender: String,
    val mottager: String,
    val filLopenummer: Int,
    val filType: String,
    val produsertDato: LocalDate,
    val beskrivelse: String,
    var rawRecord: String = "",
    var fileInfoId: Int = 0,
    var filStatus: String? = null,
    var feilTekst: String? = null
)

fun StartRecord.toFileInfo(fileName: String): FilInfo {
    return FilInfo(
        id = -1, // TODO: generert av db
        status = FileStatusValidation.OK.code,
        tilstand = FilTilstandType.OPR.name,
        anviser = this.avsender,
        filnavn = fileName,
        lopenr = this.filLopenummer,
        datoMottatt = this.produsertDato.toString(),
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = "sokos.spk.mottak",
        datoEndret = LocalDateTime.now(),
        endretAv = "sokos.spk.mottak",
        versjon = 2,
        filType = this.filType,
        feilTekst = this.feilTekst
    )
}