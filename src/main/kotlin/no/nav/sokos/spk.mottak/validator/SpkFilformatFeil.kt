package no.nav.sokos.spk.mottak.validator

enum class SpkFilformatFeil(feiltatus: String) {
    INGEN_FEIL("00"),
    AVSENDER ("01"),
    MOTTAGER ("02"),
    FILLOPENUMMER_BRUKT ("03"),
    FILLOPENUMMER ("04"),
    FILTYPE ("05"),
    RECORD_TYPE ("06"),
    ANTALL ("07"),
    SUM ("08"),
    PRODUKSJONSDATO ("09"),
    PARSING("10")
}

