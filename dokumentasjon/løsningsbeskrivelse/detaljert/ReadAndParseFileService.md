# ReadAndParseFileService
Tjenesten leser anvisningsfiler fra SPK og validerer at filformatet er riktig. Transaksjonene i filene blir lagret i en innlastingstabell T_INN_TRANSAKSJON.

Filene fra SPK blir lastet ned fra katalogen **/inbound** på en SFTP-server og sorteres etter løpenummer som inngår i filnavnet som har formatet **P611.ANV.NAV.SPK.L<løpenummer>.D<dato>.T<tid>** hvor dato = ddmmyy og tid = hhmmss

**Startbetingelse**: Dersom det finnes filer på SFTP-serveren når tjenesten starter og T_INN_TRANSAKSJON er tom, vil filene prosesseres etter hverandre i rekkefølge angitt av løpenummer i filnavnet.
For hver fil gjøres følgende behandling :

Parser hver record og gjør validering avhengig av recordtype. Hver fil skal ha en startrecord, en eller flere transaksjonrecorder og en sluttrecord. Valideringsreglene for recordtypene er angitt [her](./valideringsregler/filformatvalidering.md)
<br/> Dersom det oppstår en valideringsfeil i en fil, lages det en feilfil med et gitt format som legges på katalogen **/outbound/anvisningsretur** på SFTP-serveren. 
<br/> Dersom det er flere filer som skal behandles etter en fil som feiler, fortsetter valideringen av disse. Dersom feilen ikke er en valideringsfeil derimot, kaster tjenesten en exception og stopper videre behandling.
<br/> Ved valideringsfeil lages en feilfil med en record lik startrecord i anvisningsfilen hvor statuskode og feiltekst inneholder informasjon om valideringsfeilen. Filnavnet har formatet **SPK_NAV_<tidspunkt>_INL** hvor tidspunkt = yyyyMMdd_HHmmss. 

Når filbehandlingen er over (uavhengig av om det oppstod en valideringsfeil), blir følgende utført:
* T_LOPENR blir oppdatert med løpenummeret til anvisningsfilen dersom filen ikke inneholder løpenummerfeil, uriktig anviser (dvs ikke SPK) eller ugyldig filtype (dvs ikke ANV). 
Denne tabellen angir siste anvendte filløpenummer i løpenummersekvensen og kan følgelig ikke gjenbrukes. 
Hvis løpenummer-oppdateringen ikke kan utføres, betyr det at anvisningsfilen kan gjenbruke samme løpenummer dersom filinnholdet korrigeres og filen sendes på nytt.
* T_FIL_INFO blir oppdatert med informasjon om anvisningsfilen, slik som valideringstatus, anvisertype SPK, filtype ANV (anvisningsfil), filtilstand-status GOD (godkjent)  eller AVV (avvist), filnavn, løpenummer og feiltekst ved valideringsfeil.
* Anvisningsfilen flyttes til katalogen **/inbound/ferdig** på SFTP-serveren.

Dersom filparsingen ikke feiler, vil T_INN_TRANSAKSJON blir lastet med alle transaksjonene i filen.

Når alle transaksjonene er lest inn, blir anvisningsfilen flyttet til katalogen **/inbound/ferdig** på SFTP-serveren. 

**Filformat starterecord**

|Navn          | Format    | Verdi    | Beskrivelse                                 |
|--------------|-----------|----------|---------------------------------------------|
|rectype       |string(2)  | 01       | type record                                 |
|avsender      |string(11) | SPK      | id på avsender                              |
|mottaker      |string(11) | NAV      | id på mottaker                              |
|filløpenummer |string(6)  |          | unik per filtype og avsender                |
|filtype       |string(3)  | ANV      | type fil                                    |
|proddato      |string(8)  | yyyymmdd |                                             |
|beskrivelse   |string(35) |          | beskrivelse av filens innhold               |
|statuskode    |string(2)  |          | feil på filens innhold, brukes kun ved feil |
|feiltekst     |string(35) |          | beskrivelse av feilen, brukes kun ved feil  |

**Filformat transaksjonrecord**

| Navn          | Format     | Verdi                | Beskrivelse                                                                    |
|---------------|------------|----------------------|--------------------------------------------------------------------------------|
| rectype       | string(2)  | 02                   | type record                                                                    |
| trans-id      | string(12) |                      | unik id for transaksjonen                                                      |
| fnr           | string(11) |                      | fnr/dnr til personen med ytelse                                                |
| utbetales-til | string(11) |                      | fnr/dnr/orgnr til personen/institusjonen som mottar utbetalingen (ikke i bruk) |
| anviser-dato  | string(8)  | yyyymmdd             | type fil                                                                       |
| fomDato       | string(8)  | yyyymmdd             |                                                                                |
| tomDato       | string(8)  | yyyymmdd             |                                                                                |
| beløpstype    | string(2)  | 01 eller 02 eller 03 | type beløp                                                                     |
| beløp         | int(11)    |                      | utbetaling- eller trekkbeløp, to siste sifre er ører                           |
| art           | string(4)  |                      | pensjonsart ved utbetaling, trekkart ved trekk                                 |
| ref-transid   | string(12) |                      | ident til record som skal endres/annuleres (ikke i bruk)                       |
| tekstkode     | string(4)  |                      | (ikke i bruk)                                                                  |
| grad          | string(4)  |                      | grad for ytelsen                                                               |
| status        | string(2)  |                      | status for innlesing av transaksjonen                                          |
| feiltekst     | string(35) |                      | beskrivelse av transaksjonsfeilen                                              |


**Filformat sluttrecord:**

| Navn          | Format    | Verdi | Beskrivelse                                             |
|---------------|-----------|-------|---------------------------------------------------------|
| rectype       | string(2) | 09    | type record                                             |
| antrecords    | int(9)    |       | antall records i filen, inklusive start- og sluttrecord |
| sumbeløp      | long(14)  |       | sum beløp i filen, to siste sifre er ører               |


**Mapping T_INN_TRANSAKSJON**

| Kolonnenavn        | Verdi                                               | Kommentar                                     |
|--------------------|-----------------------------------------------------|-----------------------------------------------|
| inn_transaksjon_id |                                                     | settes av databasen                           |
| fil_info_id        | FilInfoMottak.filInfoId                             | hentes fra tabellen FIL_INFO                  |
| belopstype         | transaksjonsrecord.beløpstype                       |                                               |
| art                | transaksjonsrecord.art                              |                                               |
| fnr_fk             | transaksjonsrecord.fnr                              |                                               |
| k_transaksjon_s    | null                                                | settes i tjenesten ValidateTransaksjonService |
| utbetales_til      | transaksjonsrecord.utbetales-til                    |                                               |
| dato_fom_str       | transaksjonsrecord.fomDato                          |                                               |
| dato_tom_str       | transaksjonsrecord.tomDato                          |                                               |
| dato_fom           | transaksjonsrecord.fomDato konvertert til Date      |                                               |
| dato_tom           | transaksjonsrecord.tomDato konvertert til Date      |                                               |
| ref_transid        | transaksjonsrecord.ref-transid                      |                                               |
| belop_str          | transaksjonsrecord.beløp                            |                                               |
| belop              | transaksjonsrecord.beløp konvertert til integer     |                                               |
| tekstkode          | transaksjonsrecord.tekstkode                        |                                               |
| rectype            | transaksjonsrecord.rectype                          |                                               |
| trans_id           | transaksjonsrecord.trans-id                         |                                               |
| dato_anviser_str   | transaksjonsrecord.anviser-dato                     |                                               |
| dato_anviser       | transaksjonsrecord.anviser-dato konvertert til Date |                                               |
| avsender           | transaksjonsrecord.avsender                         |                                               |
| gradStr            | transaksjonsrecord.grad                             |                                               |
| grad               | transaksjonsrecord.grad konvertert til integer      |                                               |
| behandlet          | null                                                | settes i tjenesten ValidateTransaksjonService |
| dato_opprettet     |                                                     | dagens dato                                   |
| opprettet_av       | nais appnavn: sokos-spk-mottak                      |                                               |
| dato_endret        |                                                     | dagens dato                                   |
| endret_av          | nais appnavn: sokos-spk-mottak                      |                                               |
| Versjon            |                                                     | ikke i bruk                                   |


**Mapping T_FIL_INFO**

| Kolonnenavn      | Verdi                          | Kommentar                                                                                |
|------------------|--------------------------------|------------------------------------------------------------------------------------------|
| fil_info_id      |                                | settes av databasen                                                                      |
| k_fil_s          |                                | feilstatus settes i tjenesten ReadAndParseFileService, 00->ok, 01-10->feil               |
| k_fil_tilstand_t |                                | filtilstand settes i tjenesten ReadAndParseFileService, GOD (godkjent) eller AVV(avvist) |
| k_anviser        | startrecord.avsender           |                                                                                          |
| fil_navn         |                                | navn på mottatt fil                                                                      |
| lopenr           | startrecord.filløpenummer      | settes i tjenesten ReadAndParseFileService                                               |
| dato_mottatt     |                                | dagens dato                                                                              |
| dato_opprettet   |                                | dagens dato                                                                              |
| opprettet_av     | nais appnavn: sokos-spk-mottak |                                                                                          |
| dato_endret      |                                | dagens dato                                                                              |
| endret_av        | nais appnavn: sokos-spk-mottak |                                                                                          |
| versjon          |                                | ikke i bruk                                                                              |
| k_fil_t          | startrecord.filtype            | settes i tjenesten ReadAndParseFileService, ANV (anvisningsfil)                          |
| feiltekst        |                                | settes i tjenesten ReadAndParseFileService                                               |
