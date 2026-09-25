package no.nav.sokos.spk.mottak.domain

enum class FilStatus(
    val code: String,
    val decode: String,
    val message: String,
) {
    OK("00", "Ok", "OK"),
    UGYLDIG_ANVISER("01", "Ugyl avsend ", "Ugyldig anviser"),
    UGYLDIG_MOTTAKER("02", "Ugyldig mottaker", "Ugyldig mottaker"),
    FILLOPENUMMER_I_BRUK("03", "Løpenummer dublett", "Filløpenummer %s allerede i bruk"),
    UGYLDIG_FILLOPENUMMER("04", "Løpenummer hoppet over", "Filløpenummer format er ikke gyldig"),
    FORVENTET_FILLOPENUMMER("04", "Løpenummer hoppet over", "Forventet lopenummer %s"),
    UGYLDIG_FILTYPE("05", "Ugyl filtyp (Samme lnr må resendes)", "Ugyldig filtype"),
    UGYLDIG_RECTYPE("06", "Minst én ugyldig rectype", "Ugyldig recordtype"),
    UGYLDIG_ANTRECORDS("07", "Feil antall records", "Oppsummert antall records oppgitt i sluttrecord er %s og stemmer ikke med det faktiske antallet %s"),
    UGYLDIG_SUMBELOP("08", "Feil sumbeløp", "Total beløp %s stemmer ikke med summeringen av enkelt beløpene %s"),
    UGYLDIG_PRODDATO("09", "Feil datoformat", "Prod-dato (yyyymmdd) har ugyldig format"),
}
