package no.nav.sokos.spk.mottak.validator

enum class FileStatus (val code: String, val message: String) {
    OK ("00", "OK"),
    UGYLDIG_ANVISER ("01", "Ugyldig anviser"),
    UGYLDIG_MOTTAKER ("02", "Ugyldig mottaker"),
    FILLOPENUMMER_I_BRUK ("03", "Filløpenummer %s allerede i bruk"),
    UGYLDIG_FILLOPENUMMER ("04", "Filløpenummer format er ikke gyldig"),
    FORVENTET_FILLOPENUMMER ("04", "Forventet lopenummer %s"),
    UGYLDIG_FILTYPE ("05", "Ugyldig filtype"),
    UGYLDIG_RECTYPE ("06", "Ugyldig recordtype"),
    UGYLDIG_ANTRECORDS ("07", "Oppsumert antall records %s stemmer ikke med det faktiske antallet %s"),
    UGYLDIG_SUMBELOP ("08", "Total beløp %s stemmer ikke med summeringen av enkelt beløpene %s"),
    UGYLDIG_PRODDATO ("09", "Prod-dato (yyyymmdd) har ugyldig format"),
    UKJENT ("10", "Ukjent feil ved parsing av fil")
}