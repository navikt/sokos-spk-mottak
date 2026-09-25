package no.nav.sokos.spk.mottak.validator

const val INVALID_DATE_QUERY_PARAMETER_MESSAGE = "datoFom må være på formatet yyyy-MM-dd"

fun String.validateDateQueryParameter(): String {
    require(this.matches(Regex("""^\d{4}-(0[1-9]|1[0-2])-(0[1-9]|[12]\d|3[01])$"""))) {
        INVALID_DATE_QUERY_PARAMETER_MESSAGE
    }
    return this
}
