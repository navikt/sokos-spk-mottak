package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.modell.StartRecord
import no.nav.sokos.spk.mottak.service.FileState
import java.time.LocalDateTime
import no.nav.sokos.spk.mottak.validator.ValidationFileStatus

data class FileInfo(
    var id: Int = -1,
    val status: String,
    val tilstand: String,
    val anviser: String,
    val filnavn: String,
    val lopenr: Int,
    val datoMottatt: String? = null,
    val datoOpprettet: LocalDateTime,
    val opprettetAv: String,
    val datoEndret: LocalDateTime,
    val datoSendt: String? = null,
    val endretAv: String,
    val versjon: Int,
    val filType: String,
    val feilTekst: String?
)

fun fileInfoFromStartRecord(startRecord: StartRecord, fileName: String): FileInfo {
    return FileInfo(
        id = -1, // TODO: generert av db
        status = ValidationFileStatus.OK.code,
        tilstand = FileState.OPR.name,
        anviser = startRecord.avsender,
        filnavn = fileName,
        lopenr = startRecord.filLopenummer,
        datoMottatt = startRecord.produsertDato.toString(),
        datoOpprettet = LocalDateTime.now(),
        opprettetAv = "sokos.spk.mottak",
        datoEndret = LocalDateTime.now(),
        endretAv = "sokos.spk.mottak",
        versjon = 2,
        filType = startRecord.filType,
        feilTekst = startRecord.feilTekst
    )
}