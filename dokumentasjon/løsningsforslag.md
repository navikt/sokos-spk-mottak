# Løsningsforslag

````mermaid
flowchart TB
    spk("SPK")
    sftp("Disk")
    mot("sokos-spk-mottak")
    pesys("Pesys")
    oppdragZ("Oppdrag Z")
    spk -- Sender fil --> sftp
    sftp -- Sender feilfil --> spk
    sftp -- Leser fil --> mot
    mot -- Fnr --> pesys
    pesys -- Fullmaktsmottakere --> mot
    mot -- Utbetaling- og trekk-transaksjoner --> oppdragZ
    mot -- hent oppdragssimmulering --> oppdragZ
    mot -- Sender returfil med transaksjonstatuser -->spk
````

* **sokos-spk-mottak &rarr; pesys** 
  * Tidligere har vi sendt til Pesys via ESB. Vi skal nå bort fra ESB og benytte nytt REST-grensesnitt
* **sokos-spk-mottak &rarr; OppdragZ**
  * Her skal vi sende transaksjonene til Oppdrag Z på IBM MQ

## Sokos-spk-mottak

1. Leser fra fil
   1. Validerer filen
      1. Hvis filen er korrupt, sendes returfil med feilinformasjon til spk
      2. Hvis filen er OK, skrives transaksjonene til tabell inn-transaksjon
      3. Siden fil-validering og skriving til tabell inn-transakson skjer parallelt, må transaksjonene fjernes fra tabellen dersom valideringen feiler
2. Oppdatere filstatus (ok eller avvist) i tabell om filinformasjon (fil-info)
3. Henter fullmaktsmottakere for samtlige fnr i transaksjonene
4. Validere transaksjonene
   1. Henter status på oppdragssimulering i OppdragZ for nye utbetalingstransaksjoner for å sjekke om de er allerede er prosessert  
   2. Transaksjon OK -> Skriver godkjent transaksjon til tabell transaksjon og oppdatere status i tabell inn-transaksjon
   3. Transaksjon AVVIST -> Skriver avvist transaksjon til tabell avv-transaksjon og oppdatere status i tabell inn-transaksjon
5. Henter alle transaksjoner (avviste eller godkjente) og lager en fil som sendes tilbake til SPK
6. Sletter alle transaksjoner fra tabell inn-transaksjon når filen er opprettet
7. Sender transaksjonene (både utbetalinger og trekk) til OppdragZ via IBM MQ

(Lage kartdiagram over alt som skal skje i sokos-spk-mottak)
