package no.nav.sokos.spk.mottak.domain.record

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.validator.FileStatus

data class StartRecord(
    val avsender: String,
    val mottager: String,
    val filLopenummer: Int,
    val filType: String,
    val produsertDato: LocalDate? = null,
    val beskrivelse: String,
    val rawRecord: String,
    val fileStatus: FileStatus
)

fun StartRecord.toFileInfo(fileName: String, filTilstandType: String, filStatus: String, feiltekst: String? = null): FilInfo {
    return FilInfo(
        filStatus = filStatus,
        filTilstandType = filTilstandType,
        anviser = this.avsender,
        filType = this.filType,
        filNavn = fileName,
        lopenr = this.filLopenummer,
        feiltekst = feiltekst,
        datoMottatt = this.produsertDato,
        datoSendt = null,
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = PropertiesConfig.Configuration().naisAppName,
        datoEndret = LocalDateTime.now(),
        endretAv = PropertiesConfig.Configuration().naisAppName,
        versjon = 1
    )
}