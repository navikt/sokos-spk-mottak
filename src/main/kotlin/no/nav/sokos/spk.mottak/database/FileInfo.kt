package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.modell.StartRecord
import no.nav.sokos.spk.mottak.service.FileState
import java.time.Instant

data class FileInfo(
    var id: Int = -1,
    val status: String?,
    val tilstand: String,
    val anviser: String,
    val filnavn: String,
    val lopenr: Int,
    val datoMottatt: String,
    val datoOpprettet: String,
    val opprettetAv: String,
    val datoEndret: String,
    val datoSendt: String? = null,
    val endretAv: String,
    val versjon: Int,
    val filType: String,
    val feilTekst: String?
)

fun fileInfoFromStartRecord(startRecord: StartRecord, fileName: String): FileInfo {
    return FileInfo(
        id = -1, // TODO: generert av db
        status = startRecord.filStatus,
        tilstand = FileState.OPR.name,
        anviser = startRecord.avsender,
        filnavn = fileName,
        lopenr = startRecord.filLopenummer,
        datoMottatt = startRecord.produsertDato.toString(),
        datoOpprettet = Instant.now().toString(),
        opprettetAv = "sokos.spk.mottak",
        datoEndret = Instant.now().toString(),
        endretAv = "sokos.spk.mottak",
        versjon = 2,
        filType = startRecord.filType,
        feilTekst = startRecord.feilTekst
    )
}