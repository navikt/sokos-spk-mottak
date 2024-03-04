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
    spk -- Henter transaksjonsstatus fil --> sftp
    sftp -- Leser fil --> mot
    mot -- Fnr --> pesys
    pesys -- Fullmaktsmottakere --> mot
    mot -- Utbetaling- og trekk-transaksjoner --> oppdragZ
    mot -- Hent oppdragssimulering --> oppdragZ
    mot -- Sender transaksjonstatus fil --> sftp
    mot -- Sender tilbake feilfil dersom ikke validert --> sftp
````

* **sokos-spk-mottak &rarr; pesys** 
  * Tidligere sendt til Pesys via ESB. Skal erstattes med nytt REST-grensesnitt
* **sokos-spk-mottak &rarr; OppdragZ**
  * Sender alle transaksjoner til Oppdrag Z på MQ

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
