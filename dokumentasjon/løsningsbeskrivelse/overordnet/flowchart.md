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