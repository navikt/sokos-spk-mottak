````mermaid
flowchart TB
    spk("SPK")
    sftp("SFTP")
    mottak("sokos-spk-mottak")
    OS("OS")
    UR("UR")
    avstemming("Avstemmingskomponenten")
    spk -- sender anvisningsfil --> sftp
    sftp -- overfører feilfil ved anvisningsfilfeil --> spk
    sftp -- overfører innlesningsreturfil med status på transaksjoner --> spk
    sftp -- overfører avregningsfil --> spk
    sftp -- leser anvisningsfil --> mottak
    mottak -- sender utbetaling - og trekktransaksjoner --> OS
    mottak -- sender feilfil ved anvisningsfilfeil --> sftp
    mottak -- sender innlesningsreturfil med status på transaksjoner --> sftp
    mottak -- sender avregningsfil --> sftp
    mottak -- sender avstemmingsdata --> avstemming
    UR -- sender avregningsdata --> mottak
    
````