package no.nav.sokos.spk.mottak.domain.record

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilTilstandType
import no.nav.sokos.spk.mottak.validator.FileStatus

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
        status = FileStatus.OK.code,
        tilstand = FilTilstandType.OPR.name,
        anviser = this.avsender,
        filnavn = fileName,
        lopenr = this.filLopenummer,
        datoMottatt = this.produsertDato.toString(),
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = PropertiesConfig.Configuration().naisAppName,
        datoEndret = LocalDateTime.now(),
        endretAv = PropertiesConfig.Configuration().naisAppName,
        versjon = 2,
        filType = this.filType,
        feilTekst = this.feilTekst
    )
}