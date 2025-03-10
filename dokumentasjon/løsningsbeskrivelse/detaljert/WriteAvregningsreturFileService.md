# WriteAvregningsreturFileService

Tjenesten produserer en returfil for avregning fra UR. Filen har samme mal som *innlesningsretur*. Kontoer som starter med *0083* og *0084* er SPK kontoer som spk-mottak ikke har noen data eller
person tilkyntting til. Disse avregning blir registrert gjennom andre kanaler og skal returneres til SPK.

**Startbetingelse:** Dersom *T_RETUR_TIL_ANV* inneholder transaksjoner som ikke har sendt til SFTP-serveren, dvs kolonnen *FIL_INFO_UT_ID* i *T_RETUR_TIL_AVN* er null.

Alle avregningsretur transaksjoner som ikke sent til SFTP server produsert en returfil for:

* Hent **SISTE_LOPENR** fra *T_LOPENR* der *K_ANVISER = SPK* og *K_FIL_T = AVR* og oppdatere **SISTE_LOPENR** med +1.
* Oppretter en ny rad i *T_FIL_INFO* med informasjon om returfilen slik som filnavn, *K_FIL_T = AVR (avregningsretur)* og *K_FIL_TILSTAND = RET (returnert)* med lopenr fra forrige steget.
* Produserer en returfil som inneholder en startrecord, transaksjonsrecord og sluttrecord som blir vist under tabellen. Filnavn = SPK_NAV_\<tidspunkt>_AVR hvor tidspunkt = yyyyMMdd_HHmmss.
* Oppdater *T_RETUR_TIL_ANV* med *FIL_INFO_UT_ID = FILINFO_ID* fra forrige steget der vi opprettet ny rad i *T_FIL_INFO*.

Ved feil i behandlingen stoppes tjenesten, så vil det ikke blir oppdatert i DB2 database eller lastet opp filen til SFTP-serveren og selve prosessen vil ikke re-kjøres før det blir trigget gjennom en
RestApi kall eller ved neste skeduerlingen.

### Format til avregningsretur

| Type               | Beskrivelse                                                                  | Format                       | 
|--------------------|------------------------------------------------------------------------------|------------------------------|
| Startrecord        | Første linje i filen. Inneholder informasjon om filen, avsender og mottaker. | Se Format startrecord        |
| Transaksjonsrecord | Det skrives én transaksjonsrecord for hver transaksjon i oversendingen       | Se format transaksjonsrecord |
| Sluttrecord        | Siste linje i filen. Inneholder summering av antall records og beløp         | Se format sluttrecord        |

### Format til startrecord

| Navn          | Format/lengde | Verdi                    | Beskrivelse                                                 | 
|---------------|---------------|--------------------------|-------------------------------------------------------------|
| RECTYPE       | PIC X(02)     | "01"                     | Angir type record                                           |
| AVSENDER      | PIC X(11)     | "NAV"                    | TP-nr, orgnr eller en annen valgt ID på avsender.           |
| MOTTAKER      | PIC X(11)     | "SPK"                    | TP-nr, orgnr eller en annen valgt ID på mottaker.           |
| FILLØPENUMMER | PIC X(06)     | FilInfoMottak.lopenummer | Unik per filtype og anviser                                 |
| FILTYPE       | PIC X(03)     | "AVR"                    | Angir type fil                                              |
| PRODDATO      | PIC X(08)     | Dagens dato              | Dato filen ble produsert hos avsender (YYYYMMDD)            |
| BESKRIVELSE   | PIC X(35)     | "Avregningsretur"        | Beskrivelse av filens innhold                               |
| STATUSKODE    | PIC X(02)     | "00"                     | Viser feil på filnivå. Hele filen blir avvist hvis ulik 00. |
| FEILTEKST     | PIC X(35)     | NULL                     | Inneholder presisering av feilen for bruk i feilanalyse.    |

### Format til transaksjonsrecord

| Navn            | Format/lengde | Verdi                                | Beskrivelse                                                                                                                                                                 | 
|-----------------|---------------|--------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| RECTYPE         | PIC X(02)     | "02"                                 | Angir type record                                                                                                                                                           |
| TRANS-ID        | PIC X(12)     | ReturTilAnviserMottak.transEksId     | Trans-id mottatt fra anviser.                                                                                                                                               |
| AVSENDER-DATO   | PIC X(08)     | ReturTilAnviserMottak.datoAvsender   | Avsenders dato mottatt fra anviser (YYYYMMDD).                                                                                                                              |
| FNR/DNR         | PIC X(11)     | ReturTilAnviserMottak.gjelderId      | FNR/DNR mottatt fra avsender. Rettighetshaver for ytelsen.                                                                                                                  |
| UTBETALES-TIL   | PIC X(11)     | ReturTilAnviserMottak.utbetalesTil   | Person eller organisasjon som har mottatt utbetalingen                                                                                                                      |
| STATUS-DATO     | PIC X(08)     | ReturTilAnviserMottak.datoStatus     | Dato for når returnert status er satt (YYYYMMDD).                                                                                                                           |
| STATUS          | PIC X(04)     | ReturTilAnviserMottak.status         | Status sendt til anviser. Vil være statuser fra ulike deler av økonomiløsningen.                                                                                            |
| STATUS-TEKST    | PIC X(35)     | ReturTilAnviserMottak.statusTekst    | Beskrivelse av status. Kan også nyttes til annen informasjon som er nyttig å returnere.                                                                                     |
| BILAGSNR-SERIE  | PIC X(04)     | ReturTilAnviserMottak.bilagsnrSerie  | Bilagsnr serie fra UR                                                                                                                                                       |
| BILAGSNR        | PIC X(10)     | ReturTilAnviserMottak.bilagsnr       | Bilagsnr fra UR dvs. unik identikasjon i UR. NAVs unike ID i UR.                                                                                                            |
| KONTO           | PIC X(09)     | ReturTilAnviserMottak.konto          | Regnskapskonto som transaksjonen er ført på hos NAV.                                                                                                                        |
| FOMDATO         | PIC X(08)     | ReturTilAnviserMottak.datoFom        | Funksjonell periode mottatt av anviser                                                                                                                                      |
| TOMDATO         | PIC X(08)     | ReturTilAnviserMottak.datoTom        | Funksjonell periode mottatt av anviser                                                                                                                                      |
| BELØP           | PIC9(09)V(02) | ReturTilAnviserMottak.belop          | Utbetaling – brutto ytelse mottatt fra anviser Trekk – trukket beløp(Uten fortegn, nullfylt)                                                                                |
| DEBET/KREDIT    | PIC X(01)     | ReturTilAnviserMottak.debetKredit    | Angir fortegnet på transaksjonen D=debet (positivt tall) K=kredit (negativt tall)                                                                                           |
| UTBETALING-TYPE | PIC X(03)     | ReturTilAnviserMottak.utbetalingType | Forteller hvordan utbetalingen er sendt, norsk konto, norsk utbetalingskort, utenlandsk konto, utenlandsks sjekk etc. Kan benyttes for å vurdere statusen på transaksjonen. |
| DATO_VALUTERT   | PIC X(08)     | ReturTilAnviserMottak.datoValutering | Dato for når transaksjonen er valutert i økonomiløsningen.                                                                                                                  |

### Format til sluttrecord

| Navn       | Format/lengde   | Verdi                  | Beskrivelse                                     | 
|------------|-----------------|------------------------|-------------------------------------------------|
| RECTYPE    | PIC X(02)       | "09"                   | Angir type record                               |
| ANTRECORDS | PIC X(09)       | Sum antall records     | Antall poster i filen, inklusive start og stopp |
| SUMBELØP   | PIC 9(12)v9(02) | Sum beløp alle records | Sum beløp i filen                               |

### Eksempel til avregningsreturfil

```
01NAV        SPK        000011AVR20250304Avregningsretur                    00                                   
02123116468   202412232512594620025125946200202501090010                                   00100794088716008409019202501012025013100000207800KBK120250109
02123058138   202412232702724267227027242672202501090010                                   00100794103111008404500202501012025013100000593300DBK120250109
02123058143   202412232702724267227027242672202501090010                                   00100794103111008409019202501012025013100000011500KBK120250109
0900000000300000000812600
```