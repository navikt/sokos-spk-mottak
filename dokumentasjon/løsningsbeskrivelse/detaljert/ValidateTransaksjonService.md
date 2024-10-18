# ValidateTransaksjonService

Tjenesten behandler transaksjoner som er blitt lastet inn i T_INN_TRANSAKSJON i tjenesten ReadAndParseFileService. Alle transaksjoner som valideres ok lastes inn i T_TRANSAKSJON, mens de som feiler
lastes inn i T_AVV_TRANSAKSJON.

Startbetingelse: Dersom T_INN_TRANSAKSJON inneholder transaksjoner som ikke er behandlet, dvs kolonnen BEHANDLET i T_INN_TRANSAKSJON er N.
Følgende valideringstrinn foretas:

* Validering av fnr. Dersom fnr ikke finnes i T_PERSON blir det foretatt en forespørsel mot PDL for å sjekke om fnr tilhører en ny person eller om det har vært en endring av fnr.
  <br/> Dersom det er et nytt fnr, opprettes en ny person i T_PERSON og dersom det har vært en fnr-endring blir personen oppdatert i T_PERSON. Dersom fnr er ukjent blir transaksjonen(e) med dette fnr
  registrert med status 02 (UGYLDIG_FNR) i T_INN_TRANSAKSJON.
* Validerer alle transaksjonene etter angitte regler og oppdaterer T_INN_TRANSAKSJON med valideringsstatusene. Valideringsreglene er angitt [her](./valideringsregler/transaksjonsvalidering.md)
* Leser transaksjonene batchvis fra T_INN_TRANSAKSJON og bestemmer transaksjonstolkning. Dersom en person ikke har historikk i T_TRANSAKSJON og har to eller flere transaksjoner i T_INN_TRANSAKSJON som
  tilhører samme fagområde (PENSPK eller UFORESPK), får kun den første transaksjonen satt kolonnen K_TRANS_TOLKNING til NY, de andre blir tildelt verdien NY_EKSIST. Denne informasjonen styrer format
  og innhold på utbetalingsmeldingene som sendes til OS i tjeneste 4. Dersom forrige utbetalingstransaksjon med samme personId har et annet fnr, settes FNR_ENDRET til 1 som er styrende for formatet på
  utbetalingsmeldingen som genereres i tjeneste 4.
* Skriver transaksjonene batchvis til T_TRANSAKSJON dersom valideringstatus er ok. Tabellen T_TRANS_TILSTAND blir også oppdatert med informasjon om de nye transaksjonene. Dersom
  transaksjonsvalideringen feiler, vil transaksjonen i stedet bli skrevet til T_AVV_TRANSAKSJON.
  Når alle transaksjoner i batchen er ferdig behandlet, settes transaksjonenes kolonne BEHANDLET til J i T_INN_TRANSAKSJON for å indikere at de er ferdigbehandlet.

Ved feil i behandlingen, stoppes tjenesten, men alle transaksjoner som allerede har blitt riktig prosessert innenfor en batch er blitt oppdatert i T_TRANSAKSJON.
Ved re-skedulering vil tjenesten fortsette med de transaksjonene som ikke er behandlet, dvs transaksjoner med kolonnen BEHANDLET lik N.

**Mapping T_TRANSAKSJON**

| Kolonnenavn       | Verdi                                | Kommentar                                                               |
|-------------------|--------------------------------------|-------------------------------------------------------------------------|
| transaksjon_id    | T_INN_TRANSAKSJON.inn_transaksjon_id |                                                                         |
| person_id         | T_PERSON.person_Id                   | settes via T_INN_TRANSAKSJON.fnr_fk                                     |
| trans_tilstand_id | T_TRANS_TILSTAND.trans_tilstand_id   | settes til id’en til tilstanden som opprettes for den nye transaksjonen |
| k_trans_tilst_t   | OPR                                  | opprettet                                                               |
| k_transaksjon_s   | T_INN_TRANSAKSJON.k_transaksjon_s    |                                                                         |
| k_belop_t         | T_INN_TRANSAKSJON.belopstype         |                                                                         |
| k_art             | T_INN_TRANSAKSJON.art                |                                                                         |
| fil_info_id       | T_INN_TRANSAKSJON.fil_info_id        |                                                                         |
| k_anviser         | T_INN_TRANSAKSJON.avsender           |                                                                         |
| tekstkode         | T_INN_TRANSAKSJON.tekstkode          | ikke i bruk                                                             |
| utbetales_til     | T_INN_TRANSAKSJON.utbetales_til      | ikke i bruk                                                             |
| dato_fom          | T_INN_TRANSAKSJON.dato_fom           |                                                                         |
| dato_tom          | T_INN_TRANSAKSJON.dato_tom           |                                                                         |
| dato_person_fom   | 1900-01-01                           |                                                                         |
| dato_reak_fom     | 1900-01-01                           | ikke i bruk                                                             |
| fnr_fk            | T_INN_TRANSAKSJON.fnr_fk             |                                                                         |
| fnr_endret        |                                      | settes i ValidateTransaksjonService                                     |
| ref_trans_id      | T_INN_TRANSAKSJON.ref_trans_id       |                                                                         |
| trekkvedtak_id_fk | null                                 |                                                                         |
| mot_id            | T_INN_TRANSAKSJON.inn_transaksjon_id |                                                                         |
| os_id_fk          | null                                 |                                                                         |
| os_linje_id_fk    | null                                 |                                                                         |
| os_status         | null                                 |                                                                         |
| belop             | T_INN_TRANSAKSJON.belop              |                                                                         |
| rectype           | T_INN_TRANSAKSJON.rectype            |                                                                         |
| k_trans_tolkning  |                                      | settes i ValidateTransaksjonService                                     |
| trans_eks_id_fk   | T_INN_TRANSAKSJON.trans_id_fk        |                                                                         |
| dato_anviser      | T_INN_TRANSAKSJON.dato_anviser       |                                                                         |
| saldo             | T_INN_TRANSAKSJON.saldo              | ikke i bruk                                                             |
| prioritet         | T_INN_TRANSAKSJON.prioritet          | ikke i bruk                                                             |
| kid               | T_INN_TRANSAKSJON.kid                | ikke i bruk                                                             |
| k_trekkansvar     | T_INN_TRANSAKSJON.trekkansvar        | ikke i bruk                                                             |
| grad              | T_INN_TRANSAKSJON.grad               |                                                                         |
| dato_opprettet    |                                      | dagens dato                                                             |
| opprettet_av      | nais appnavn: sokos-spk-mottak       |                                                                         |
| dato_endret       |                                      | dagens dato                                                             |
| endret_av         | nais appnavn: sokos-spk-mottak       |                                                                         |
| versjon           |                                      | ikke i bruk                                                             |

**Mapping T_TRANS_TILSTAND**

| Kolonnenavn       | Verdi                          | Kommentar                           |
|-------------------|--------------------------------|-------------------------------------|
| trans_tilstand_id |                                | settes av databasen                 |
| transaksjon_id    | T_TRANSAKSJON.transaksjon_id   | id fra den opprettede transaksjonen |
| k_trans_tilst_t   | OPR                            |                                     |
| dato_opprettet    |                                | dagens dato                         |
| opprettet_av      | nais appnavn: sokos-spk-mottak |                                     |
| dato_endret       |                                | dagens dato                         |
| endret_av         | nais appnavn: sokos-spk-mottak |                                     |
| versjon           |                                | ikke i bruk                         |
