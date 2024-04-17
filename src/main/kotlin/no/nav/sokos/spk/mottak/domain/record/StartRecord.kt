package no.nav.sokos.spk.mottak.domain.record

import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.FilInfo

data class StartRecord(
    val avsender: String,
    val mottager: String,
    val filLopenummer: Int,
    val filType: String,
    val produsertDato: LocalDate,
    val beskrivelse: String,
    val rawRecord: String
)

fun StartRecord.toFileInfo(fileName: String, filTilstandType: String, filStatus: String, feiltekst: String? = null): FilInfo {
    val systemId = PropertiesConfig.Configuration().naisAppName
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
        opprettetAv = systemId,
        datoEndret = LocalDateTime.now(),
        endretAv = systemId,
        versjon = 1
    )
}