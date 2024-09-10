package no.nav.sokos.spk.mottak.domain

const val SPK = "SPK"
const val NAV = "NAV"
const val MOT = "MOT"
const val SPK_TSS = "80000427901"

// FILETYPE
const val FILTYPE_ANVISER = "ANV"
const val FILTYPE_INNLEST = "INL"

// FILTILSTANDTYPE
const val FILTILSTANDTYPE_AVV = "AVV"
const val FILTILSTANDTYPE_GOD = "GOD"
const val FILTILSTANDTYPE_RET = "RET"

// TRANSAKSJONTYPE
const val TRANSAKSJONSTATUS_OK = "00"

// RECTYPE
const val RECTYPE_STARTRECORD = "01"
const val RECTYPE_TRANSAKSJONSRECORD = "02"
const val RECTYPE_SLUTTRECORD = "09"

// BEHANDLET
const val BEHANDLET_NEI = "N"
const val BEHANDLET_JA = "J"

// BELOPTYPE
const val BELOPSTYPE_SKATTEPLIKTIG_UTBETALING = "01"
const val BELOPSTYPE_IKKE_SKATTEPLIKTIG_UTBETALING = "02"
const val BELOPSTYPE_TREKK = "03"

val BELOPTYPE_TIL_OPPDRAG = listOf(BELOPSTYPE_SKATTEPLIKTIG_UTBETALING, BELOPSTYPE_IKKE_SKATTEPLIKTIG_UTBETALING)
val BELOPTYPE_TIL_TREKK = listOf(BELOPSTYPE_TREKK)

// TRANSAKSJON TOLKNING
const val TRANS_TOLKNING_NY = "NY"
const val TRANS_TOLKNING_NY_EKSIST = "NY_EKSIST"

// FNR ENDRET
const val FNR_ENDRET = '1'
const val FNR_IKKE_ENDRET = '0'

// TRANSAKSJON TILSTAND STATUS
const val TRANS_TILSTAND_OPPRETTET = "OPR"
const val TRANS_TILSTAND_MANUELT_KORRIGERT_RESEND = "MKR"
const val TRANS_TILSTAND_OPPDRAG_SENDT_OK = "OSO"
const val TRANS_TILSTAND_OPPDRAG_SENDT_FEIL = "OSF"
const val TRANS_TILSTAND_OPPDRAG_RETUR_OK = "ORO"
const val TRANS_TILSTAND_OPPDRAG_RETUR_FEIL = "ORF"
const val TRANS_TILSTAND_TREKK_SENDT_OK = "TSO"
const val TRANS_TILSTAND_TREKK_SENDT_FEIL = "TSF"
const val TRANS_TILSTAND_TREKK_RETUR_OK = "TRO"
const val TRANS_TILSTAND_TREKK_RETUR_FEIL = "TRF"
const val TRANS_TILSTAND_OPPDRAG_AVSTEMMING = "OAO"

val TRANS_TILSTAND_TIL_OPPDRAG = listOf(TRANS_TILSTAND_OPPRETTET, TRANS_TILSTAND_OPPDRAG_SENDT_FEIL, TRANS_TILSTAND_MANUELT_KORRIGERT_RESEND)
val TRANS_TILSTAND_TIL_TREKK = listOf(TRANS_TILSTAND_OPPRETTET, TRANS_TILSTAND_TREKK_SENDT_FEIL, TRANS_TILSTAND_MANUELT_KORRIGERT_RESEND)

// ANVISER FIL BESKRIVELSE
const val ANVISER_FIL_BESKRIVELSE = "Innlesingsretur"
