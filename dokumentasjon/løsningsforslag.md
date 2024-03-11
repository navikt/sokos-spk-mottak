# Løsningsforslag

````mermaid
flowchart TB
    spk("SPK")
    sftp("SFTP")
    mot("sokos-spk-mottak")
    pesys("Pesys")
    oppdragZ("Oppdrag Z")
    spk -- Sender fil --> sftp
    spk -- Henter fil som ikke ble validert --> sftp
    spk -- Henter transaksjonsstatus-fil --> sftp
    sftp -- Leser fil --> mot
    mot -- Fnr --> pesys
    pesys -- Fullmaktsmottakere --> mot
    mot -- Utbetaling- og trekk-transaksjoner --> oppdragZ
    mot -- Hent oppdragsimulering --> oppdragZ
    mot -- Sender transaksjonstatus-fil --> sftp
    mot -- Sender tilbake feilfil dersom fil ikke validerer --> sftp
````

* **sokos-spk-mottak &rarr; pesys** 
  * Tidligere sendt til Pesys via ESB. Skal erstattes med nytt REST-grensesnitt
* **sokos-spk-mottak &rarr; OppdragZ**
  * Sender alle transaksjoner til Oppdrag Z på MQ

## Sokos-spk-mottak

1. Leser filer fra sftp sortert på løpenummer
    1. Oppretter informasjon om mottatt fil i tabellen fil-info
    2. Oppdaterer tabellen lopenr med løpenummer fra motatt fil
    3. Validerer filen [Valideringsregler](Filformatvalidering.md)
      1. Hvis filen har formatsfeil, lages en fil med feilinformasjon som sendes SPK
      2. Hvis filen har riktig format, skrives transaksjonene til tabellen inn-transaksjon
      3. Siden fil-validering og skriving til tabell skjer i parallell, må transaksjoner fjernes fra tabellen inn-transaksjon dersom valideringen feiler
      4. Oppdaterer filstatus (ok eller avvist) i fil-info
3. Henter fullmaktsmottakere for samtlige fnr i transaksjonene fra en rest-tjeneste i pesys
4. Validere transaksjonene
   1. Henter status på oppdragssimulering fra OppdragZ for nye utbetalingstransaksjoner for å sjekke om de allerede er prosessert  
   2. Transaksjon OK -> Skriver godkjent transaksjon til tabell transaksjon og oppdatere status i tabell inn-transaksjon
   3. Transaksjon AVVIST -> Skriver avvist transaksjon til tabell avv-transaksjon og oppdatere status i tabell inn-transaksjon
5. Henter alle transaksjoner (både avviste og godkjente) og lager en returfil hvor avviste transaksjoner begrunnes og sendes tilbake til SPK
6. Sletter alle transaksjoner fra tabell inn-transaksjon når returfilen er opprettet
7. Sender transaksjonene (både utbetalinger og trekk) til OppdragZ via MQ (asynkront mottak av transaksjonstatuser fra OppdragZ (som oppdaterer statuser i transaksjon-tabellen) inngår ikke i tjenesten)

