package no.nav.sokos.spk.mottak.domain

const val SPK = "SPK"
const val NAV = "NAV"
const val MOT = "MOT"
const val SPKMOT = "SPKMOT"
const val SPK_TSS = "80000427901"

// SERVICENAVN
const val AVREGNING_LISTENER_SERVICE = "AvregningListenerService"
const val AVSTEMMING_SERVICE = "AvstemmingService"
const val READ_FILE_SERVICE = "ReadFileService"
const val SEND_AVREGNINGSRETUR_SERVICE = "SendAvregningsreturService"
const val SEND_INNLESNINGSRETUR_SERVICE = "SendInnlesningsreturService"
const val SEND_TREKK_SERVICE = "SendTrekkService"
const val SEND_UTBETALING_SERVICE = "SendUtbetalingService"
const val VALIDATE_TRANSAKSJON_SERVICE = "ValidateTransaksjonService"

// FILETYPE
const val FILTYPE_ANVISER = "ANV"
const val FILTYPE_INNLEST = "INL"
const val FILTYPE_AVREGNING = "AVR"

// FILTILSTANDTYPE
const val FILTILSTANDTYPE_AVV = "AVV"
const val FILTILSTANDTYPE_GOD = "GOD"
const val FILTILSTANDTYPE_RET = "RET"

// TRANSAKSJONTYPE
const val TRANSAKSJONSTATUS_OK = "00"

// OS_STATUS
const val OS_STATUS_OK = "00"

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
const val FNR_ENDRET = "1"
const val FNR_IKKE_ENDRET = "0"

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
const val TRANS_TILSTAND_OPPDRAG_AVSTEMMING_OK = "OAO"

val TRANS_TILSTAND_TIL_OPPDRAG = listOf(TRANS_TILSTAND_OPPRETTET, TRANS_TILSTAND_OPPDRAG_SENDT_FEIL, TRANS_TILSTAND_MANUELT_KORRIGERT_RESEND)
val TRANS_TILSTAND_TIL_TREKK = listOf(TRANS_TILSTAND_OPPRETTET, TRANS_TILSTAND_TREKK_SENDT_FEIL, TRANS_TILSTAND_MANUELT_KORRIGERT_RESEND)

// FIL BESKRIVELSE
const val ANVISER_FIL_BESKRIVELSE = "Innlesingsretur"
const val AVREGNING_FIL_BESKRIVELSE = "Avregningsretur"
