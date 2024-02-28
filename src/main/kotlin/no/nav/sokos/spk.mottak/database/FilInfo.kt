package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.modell.FirstLine
import no.nav.sokos.spk.mottak.service.FilTilstandType
import java.time.Instant

data class FilInfo(
    val id: Int, // generert av db
    val status: String?,
    val tilstand: String,
    val anviser: String,
    val filnavn: String,
    val lopenr: Int,
    val datoMottatt: String,
    val datoOpprettet: String,
    val opprettetAv: String,
    val datoEndret: String? = null,
    val endretAv: String? = null,
    val versjon: String? = null,
    val filType: String,
    val feilTekst: String?
) {
    override fun toString(): String {
        return "FilInfo(id=$id, status=$status, tilstand='$tilstand', anviser='$anviser', filnavn='$filnavn', lopenr=$lopenr, datoMottatt='$datoMottatt', datoOpprettet='$datoOpprettet', opprettetAv='$opprettetAv', datoEndret=$datoEndret, endretAv=$endretAv, versjon=$versjon, filType='$filType', feilTekst=$feilTekst)"
    }
}

fun filInfoFraStartLinje(startLinje: FirstLine, filnavn: String): FilInfo {
    return FilInfo(
        id = 0, // generert av db
        status = startLinje.filStatus,
        tilstand = FilTilstandType.OPR.name,
        anviser = startLinje.avsender,
        filnavn = filnavn,
        lopenr = startLinje.filLopenummer,
        datoMottatt = startLinje.produsertDato.toString(),
        datoOpprettet = Instant.now().toString(),
        opprettetAv = "sokos.spk.mottak",
        datoEndret = Instant.now().toString(),
        endretAv = "sokos.spk.mottak",
        filType = startLinje.filType,
        feilTekst = startLinje.feilTekst
    )
}