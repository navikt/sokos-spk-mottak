````mermaid
flowchart TB
    spk("SPK")
    sftp("SFTP")
    mottak("sokos-spk-mottak")
    oppdragZ("Oppdrag Z")
    ur("UR Z")
    avstemming("Avstemmingskomponenten")
    spk -- sender anvisningsfil --> sftp
    sftp -- overfører feilfil ved anvisningsfilfeil --> spk
    sftp -- overfører returfil med status på transaksjoner --> spk
    sftp -- leser anvisningsfil --> mottak
    mottak -- sender utbetaling - og trekktransaksjoner --> oppdragZ
    mottak -- sender feilfil ved anvisningsfilfeil --> sftp
    mottak -- sender returfil med status på transaksjoner --> sftp
    mottak -- sender avstemmingsdata --> avstemming
    ur -- sender avregningsgrunnlag --> mottak
````