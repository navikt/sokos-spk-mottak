package no.nav.sokos.spk.mottak.validator

enum class ValidationFileStatus (val code: String, val message: String) {
    OK ("00", "OK"),
    UGYLDIG_ANVISER ("01", "Ugyldig anviser"),
    UGYLDIG_MOTTAKER ("02", "Ugyldig mottaker"),
    FILLOPENUMMER_I_BRUK ("03", "Filløpenummer allerede i bruk"),
    UGYLDIG_FILLOPENUMMER ("04", "Filløpenummer ikke lik forrige + 1"),
    UGYLDIG_FILTYPE ("05", "Ugyldig filtype"),
    UGYLDIG_RECTYPE ("06", "Ugyldig recordtype"),
    UGYLDIG_ANTRECORDS ("07", "Oppsumert antall records stemmer ikke med det faktiske antallet"),
    UGYLDIG_SUMBELOP ("08", "Total beløp stemmer ikke med summeringen av enkelt beløpene"),
    UGYLDIG_PRODDATO ("09", "Prod-dato (yyyymmdd) har ugyldig format"),
    UKJENT ("10", "Ukjent feil ved parsing av fil")
}