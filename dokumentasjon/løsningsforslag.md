# Løsningsforslag

````mermaid
flowchart TB
    sftp("Disk")
    mot("sokos-spk-mottak")
    pesys("Pesys")
    oppdragZ("Oppdrag Z")
    SPK -- Sender fil --> sftp
    sftp -- Leser fil --> mot
    mot -- Fnr --> pesys
    pesys -- Fullmaktsmottakere --> mot
    mot -- Transaksjoner --> oppdragZ
    oppdragZ -- bekrefte transkasjoner --> mot
    mot --> avviksfil("Transaksjonavviksfil")
````

* **sokos-spk-mottak &rarr; pesys** 
  * Tidligere har vi sendt til Pesys via ESB. Vi skal nå bort fra ESB og benytte REST
* **sokos-spk-mottak &rarr; OppdragZ**
  * Her skal vi sende transaksjonene til Oppdrag Z på IBM MQ

## Sokos-spk-mottak

1. Leser fra fil
   1. Validerer filen
      1. Hvis filen er korrupt legger vi filen til AVVIST mappe
      2. Hvis filen er OK, flytter vi til OK mappe
         1. Legge transaksjonene inn i en tabell
2. Oppdatere status på filinfo
   1. Oppdatere i database (status om filen er OK eller AVVIST)
3. Validere transaksjonene
   1. Transaksjon OK -> Legger i egen tabell
      1. Sjekke FNR mot fullmaktregister (Om vi sender en og en FNR eller liste, må sjekkes med PESYS hva som tilbys)
   2. Transaksjon AVVIST -> Legger i egen tabell
      1. Henter alle transaksjoner som er avvist og lager en fil for å sende tilbake til SPK
      2. Sletter alle avvist transaksjoner fra tabellen når filen er opprettet
4. Sender transaksjonene til OppdragZ via IBM MQ
   1. (avhengig av at fullmakt er OK slik at vi utbetaler til riktig konto)
5. Få tilbake status fra OppdragZ om transaksjonene er OK


(Lage kartdiagram over alt som skal skje i sokos-spk-mottak)
