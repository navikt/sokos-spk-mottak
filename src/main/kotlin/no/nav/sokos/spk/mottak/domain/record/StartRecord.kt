package no.nav.sokos.spk.mottak.domain.record

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.SPK
import java.time.LocalDate
import java.time.LocalDateTime

data class StartRecord(
    val avsender: String,
    val mottager: String,
    val filLopenummer: String,
    val filType: String,
    val produsertDato: LocalDate? = null,
    val beskrivelse: String,
    val kildeData: String,
    val filStatus: FilStatus,
)

fun StartRecord.toFileInfo(
    filNavn: String,
    filTilstandType: String,
    filStatus: String,
    feilTekst: String? = null,
): FilInfo {
    val systemId = PropertiesConfig.Configuration().naisAppName
    return FilInfo(
        filStatus = filStatus,
        filTilstandType = filTilstandType,
        anviser = SPK,
        filType = this.filType,
        filNavn = filNavn,
        lopeNr = this.filLopenummer,
        feilTekst = feilTekst,
        datoMottatt = this.produsertDato,
        datoSendt = null,
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = systemId,
        datoEndret = LocalDateTime.now(),
        endretAv = systemId,
        versjon = 1,
    )
}
