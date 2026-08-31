## Validering av anvisningsfiler fra SPK

Anvisningsfiler fra SPK sendes til en sftp-server hvor de leses periodisk av **sokos-spk-mottak** og behandles i rekkefølge basert på filnavnets løpenummer.
Valideringen på filen gjøres på følgende felter:

| Valideringsregel                                                     | Feilstatus |
|----------------------------------------------------------------------|------------|
| startrecord:anviser = '**SPK**'                                      | 01         |
| startrecord:mottaker = '**NAV**'                                     | 02         |
| startrecord:filløpenummer eksisterer ikke for anviser SPK            | 03         |
| startrecord:filløpenummer = forrige godkjente løpenummer fra SPK + 1 | 04         |
| startrecord:filtype = '**ANV**'                                      | 05         |
| startrecord:rectype = '**01**'                                       | 06         |
| startrecord:proddato har format = **yyyymmdd**                       | 09         |
| sluttrecord:rectype = '**09**'                                       | 06         |
| sluttrecord:antrecords = antall records i filen                      | 07         |
| sluttrecord:sumbelop = summen av alle beløpene i filen               | 08         |
| transaksjonsrecord:rectype = '**02**'                                | 06         |

Dersom en av valideringene feiler, skal det skrives en fil med navnformat **P611.ANV.NAV.SPK.L\<løpenummer>.D\<dato>.T\<tid>** (dato = ddmmyy og tid = hhmmss) og som bare inneholder en startrecord
hvor
feltene STATUSKODE og FEILTEKST inneholder informasjon om hvorfor filen er avvist.
De andre feltene i startrecorden skal være identiske med den mottatte anvisningsfilen.
