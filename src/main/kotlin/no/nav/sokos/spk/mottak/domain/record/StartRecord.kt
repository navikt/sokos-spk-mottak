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
    val rawRecord: String
)

fun StartRecord.toFileInfo(fileName: String, filTilstandType: FilTilstandType, feilTekst: String? = null): FilInfo {
    return FilInfo(
        status = FileStatus.OK.code,
        tilstand = filTilstandType.name,
        anviser = this.avsender,
        filnavn = fileName,
        lopenr = this.filLopenummer,
        datoMottatt = this.produsertDato,
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = PropertiesConfig.Configuration().naisAppName,
        datoEndret = LocalDateTime.now(),
        endretAv = PropertiesConfig.Configuration().naisAppName,
        versjon = 1, // TODO: Trenger vi denne?
        filType = this.filType,
        feilTekst = feilTekst
    )
}