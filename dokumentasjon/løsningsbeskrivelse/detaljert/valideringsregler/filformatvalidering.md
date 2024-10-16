## Validering av anvisningsfiler fra SPK

Anvisningsfiler fra SPK sendes til en sftp-server hvor de leses periodisk av **sokos-spk-mottak** og behandles i rekkefølge basert på filnavnets løpenummer.
Valideringen på filen gjøres på følgende recorder og felter:

* Startrecord må ha anviser = '**SPK**', hvis ikke avvises filen med status '01'
* Startrecord må ha mottaker = '**NAV**', hvis ikke avvises filen med status '02'
* Startrecord må ha filløpenummer som ikke allerede eksistere for anviser SPK, hvis ikke avvises filen med status '03'
* Startrecord må ha filløpenummer som er lik forrige godkjente løpenummer fra SPK + 1, hvis ikke avvises filen med status '04'
* Startrecord må ha filtype = '**ANV**', hvis ikke avvises filen med status '05'
* Startrecord må ha rectype = '**01**', hvis ikke avvises filen med status '06'
* Startrecord må ha proddato med format **yyyymmdd**, hvis ikke avvises filen med status '09'
* Sluttrecord må ha rectype = '**09**', hvis ikke avvises filen med status '06'
* Sluttrecord må ha antrecords lik antall records i filen, hvis ikke avvises filen med status '07'
* Sluttrecord må ha sumbelop lik summen av alle beløpene i filen, hvis ikke avvises filen med status '08'
* Transaksjonsrecordene må ha rectype = '**02**', hvis ikke avvises filen med status '06'

Dersom en av valideringene feiler, skal det skrives en fil med navnformat **P611.ANV.NAV.SPK.L<løpenummer>.D<dato>.T<tid>** (dato = ddmmyy og tid = hhmmss) og som kun inneholder startrecord hvor feltene STATUSKODE og FEILTEKST inneholder informasjon om hvorfor filen er avvist.
De andre feltene i startrecorden skal være identiske med de i den mottatte anvinsingsfilen.
