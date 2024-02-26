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
   1. Validerer fila basert på valideringsregler
0. Endre status på fila i filinfotabellen
0. Legge transaksjonene inn i inn-transakjonstabellen
0. Validerer fila
   1. Validerer på transkasjonsnivå
