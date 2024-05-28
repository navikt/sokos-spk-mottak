# Løsningsforslag

````mermaid
flowchart TB
    spk("SPK")
    sftp("SFTP")
    mottak("sokos-spk-mottak")
    oppdragZ("Oppdrag Z")
    spk -- Sender anvisningsfil --> sftp
    sftp -- Henter innlesningsreturfil (INL) som ikke ble validert --> spk
    sftp -- Henter returfil (ANV) med status på transaksjoner --> spk
    sftp -- Leser anvisningsfil --> mottak
    mottak -- Utbetaling- og trekk-transaksjoner --> oppdragZ
    mottak -- Sender returfil (ANV) med status på transaksjoner --> sftp
    mottak -- Sender tilbake innlesningsreturfil (INL) dersom ikke validert --> sftp
````

* **sokos-spk-mottak &rarr; OppdragZ**
    * Sender alle transaksjoner til Oppdrag Z på MQ

## sokos-spk-mottak

1. Leser anvisningsfiler fra sftp-server `INBOUND`-mappe sortert på løpenummer
    1. Validerer filen med [filformatvalideringsregler](filformatvalidering.md)
    2. Hvis filen har formatsfeil, lages en fil med feilinformasjon som sendes til sftp-server
    3. Hvis filen har riktig format, skrives transaksjonene til tabell `T_INN_TRANSAKSJON`
    4. Oppretter informasjon om mottatt fil i tabell `T_FIL_INFO` med filstatus OK eller AVVIST
    5. Oppdaterer tabell `T_LOPENUMMER` med løpenummer fra mottatt fil
    6. Flytter anvisningsfilen til `FERDIG`-mappe på sftp-server

2. Validere transaksjonene med [transaksjonsvalideringsregler](transaksjonsvalidering.md)
    1. Hvis transaksjonen er OK -> Skriver transaksjon til tabell `T_TRANSAKSJON` og oppdatere status i tabell `T_INN_TRANSAKSJON`
    2. Hvis transaksjonen er AVVIST -> Skriver transaksjon til tabell `T_AVV_TRANSAKSJON` og oppdatere status i tabell `T_INN_TRANSAKSJON`
3. Henter alle transaksjoner som er ferdigbehandlet (både OK og AVVIST) og lager en returfil med status på transaksjonene og sendes tilbake til sftp-server
4. Tømmer tabell `T_INN_TRANSAKSJON` når returfilen er opprettet og sendt tilbake til sftp-server
5. Sender transaksjonene (både utbetalinger og trekk?) til OppdragZ via MQ

