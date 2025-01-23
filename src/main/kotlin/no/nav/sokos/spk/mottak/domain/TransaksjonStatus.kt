package no.nav.sokos.spk.mottak.domain

enum class TransaksjonStatus(
    val code: String,
    val message: String,
) {
    OK("00", "Ok"),
    TRANS_ID_DUBLETT("01", "Trans-id dublett"),
    UGYLDIG_FNR("02", "Ugyldig fødselsnummer / fødselsnummer finnes ikke"),
    UGYLDIG_DATO("03", "Ugyldig fomdato/tomdato"),
    UGYLDIG_BELOPSTYPE("04", "Ugyldig beløpstype"),
    UGYLDIG_ART("05", "Ugyldig art"),
    UGYLDIG_ANVISER_DATO("09", "Ugyldig anviser-dato"),
    UGYLDIG_BELOP("10", "Ugyldig beløp"),
    UGYLDIG_KOMBINASJON_AV_ART_BELOPSTYPE("11", "Ugyldig kombinasjon av art og beløpstype"),
    ART_MANGLER_GRAD("16", "Ugyldig grad"),
    ;

    companion object {
        fun getByCode(code: String) = entries.toTypedArray().find { it.code == code }
    }
}
