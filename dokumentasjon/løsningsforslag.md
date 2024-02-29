# Løsningsforslag

````mermaid
flowchart TB
    spk("SPK")
    sftp("Disk")
    mot("sokos-spk-mottak")
    pesys("Pesys")
    oppdragZ("Oppdrag Z")
    spk -- Sender fil --> sftp
    sftp -- Leser fil --> mot
    mot -- Fnr --> pesys
    pesys -- Fullmaktsmottakere --> mot
    mot -- Transaksjoner --> oppdragZ
    oppdragZ -- bekrefte transaksjoner --> mot
    mot -- Sender returfil -->spk
````

* **sokos-spk-mottak &rarr; pesys** 
  * Tidligere har vi sendt til Pesys via ESB. Vi skal nå bort fra ESB og benytte nytt REST-grensesnitt
* **sokos-spk-mottak &rarr; OppdragZ**
  * Her skal vi sende transaksjonene til Oppdrag Z på IBM MQ

## Sokos-spk-mottak

1. Leser fra fil
   1. Validerer filen
      1. Hvis filen er korrupt legger vi filen til AVVIST mappe
      2. Hvis filen er OK, flytter vi til OK mappe
         1. Legge transaksjonene inn i en tabell (inn-transaksjon)
2. Oppdatere status på filinfo
   1. Oppdatere i database (status om filen er OK eller AVVIST)
3. Validere transaksjonene
   1. Transaksjon OK -> Legger i egen tabell (transaksjon)
      1. Sjekke FNR mot fullmaktregister (sender enkeltvis FNR eller liste, må sjekkes med hva PESYS tilbyr)
   2. Transaksjon AVVIST -> Legger i egen tabell (avvist-transaksjon)
      1. Henter alle transaksjoner (avviste eller godkjente) og lager en fil som sendes tilbake til SPK
      2. Sletter alle transaksjoner fra tabellen inn-transaksjon når filen er opprettet
4. Sender transaksjonene (utbetalinger og trekk) til OppdragZ via IBM MQ
   1. (avhengig av at fullmakt er OK slik at utbetaling skjer til riktig konto)
5. Få tilbake status på transaksjonene asynkront fra OppdragZ


(Lage kartdiagram over alt som skal skje i sokos-spk-mottak)
