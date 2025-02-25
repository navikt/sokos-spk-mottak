# AvregningService

Tjenesten lytter på en kø som inneholder avregningsmeldinger sendt fra UR. Disse meldingene valideres, berikes, lagres og sendes til SPK.

Avregningsmeldingene inneholder grunnlagsinformasjon fra UR og brukes til å danne avregningsinformasjon som oversendes daglig til SPK som filer.
Dersom avregningsmeldingen inneholder `delYtelseId` (= utbetalingsmeldingens `mottaksId`) gjøres det et oppslag i T_TRANSAKSJON med denne parameteren som nøkkel i tillegg til `fagSystemId` (=
utbetalingsmeldingens `personId`) og `tomDato` (utbetalingperiodens til-dato).
Tidligere har `mottaksId` vært benyttet for flere enn en transaksjon og følgelig er `tomDato` en del av nøkkelen. Dagens løsning har derimot bare en `mottaksId` per transaksjon.
Parameteren `fagSystemId` brukes for å sikre at `delYtelseId` tilhører `sokos-spk-mottak`.

Hvis avregningsmeldingen ikke er knyttet til en utbetalingsmelding, men derimot en trekkmelding, vil meldingsparameteren `trekkVedtakId` brukes som nøkkel for knytting av avregningen til transaksjonen
i
T_TRANSAKSJON.

Følgende informasjonen hentes fra T_TRANSAKSJON og knyttes til avregningstransaksjonen:

* `fnrFk` (personens fødselsnummer)
* `transEksId` (transaksjonsid til SPK)
* `datoAnviser` (anvisningsdato til SPK)
* `transaksjonId` (transaksjonens id)

Hvis en avregningsmelding som er knyttet til en trekkmelding ikke finnes i T_TRANSAKSJON, vil verdien av meldingsparameteren `kreditorRef` benyttes til `transEksId` (SPK sin transaksjonsId).
Dersom man ikke klarer å knytte avregningsmeldingen til en transaksjon i T_TRANSAKSJON, vil `datoAnviser` settes til '1900-01-01'.

Avregningsmeldingen blir mappet og persistert til en rad i T_RETUR_TIL_ANV.

**Format på avregningsmelding fra UR Z:**
<br/>&emsp;avregningsgrunnlag.oppdragsId (Int(10))
<br/>&emsp;avregningsgrunnlag.linjeId (Int(5))
<br/>&emsp;avregningsgrunnlag.trekkvedtakId (Int(10))
<br/>&emsp;avregningsgrunnlag.gjelderId (String(11))
<br/>&emsp;avregningsgrunnlag.utbetalesTil (String(11))
<br/>&emsp;avregningsgrunnlag.datoStatusSatt (String(8), yyyyMMdd)
<br/>&emsp;avregningsgrunnlag.status (String(4))
<br/>&emsp;avregningsgrunnlag.bilagsnrSerie (String(4))
<br/>&emsp;avregningsgrunnlag.bilagsnr (String(10))
<br/>&emsp;avregningsgrunnlag.konto (String(9))
<br/>&emsp;avregningsgrunnlag.fomdato (String(8), yyyyMMdd)
<br/>&emsp;avregningsgrunnlag.tomdato (String(8), yyyyMMdd)
<br/>&emsp;avregningsgrunnlag.belop (Int(11))
<br/>&emsp;avregningsgrunnlag.debetKredit (String(1))
<br/>&emsp;avregningsgrunnlag.utbetalingsType (String(3))
<br/>&emsp;avregningsgrunnlag.transTekst (String(35))
<br/>&emsp;avregningsgrunnlag.datoValutert (String(8), yyyyMMdd)
<br/>&emsp;avregningsgrunnlag.delytelseId (String(10))
<br/>&emsp;avregningsgrunnlag.fagSystemId (String(30))
<br/>&emsp;avregningsgrunnlag.kreditorRef (String(30))

Eksempel:

```json
{
  "avregningsgrunnlag": {
    "oppdragsId": 70014840,
    "linjeId": 3,
    "trekkvedtakId": null,
    "gjelderId": "08410376603",
    "utbetalesTil": "08410376603",
    "datoStatusSatt": "20240219",
    "status": "0018",
    "bilagsnrSerie": "10",
    "bilagsnr": "759197901",
    "konto": "008404500",
    "fomdato": "20240201",
    "tomdato": "20240229",
    "belop": 5811,
    "debetKredit": "D",
    "utbetalingsType": "BK1",
    "transTekst": "0030 012924639",
    "datoValutert": "20240219",
    "delytelseId": "84004200",
    "fagSystemId": "1234567",
    "kreditorRef": null
  }
}
```

**Mapping T_RETUR_TIL_ANV**

| Kolonnenavn       | Verdi                              | Kommentar                                                                                                    |
|-------------------|------------------------------------|--------------------------------------------------------------------------------------------------------------|
| RETUR_TIL_ANV_ID  |                                    | settes av databasen                                                                                          |
| RECTYPE           | "02"                               | recordtype                                                                                                   |
| K_RETUR_T         | "AVR"                              | avregningstype                                                                                               |
| K_ANVISER         | "SPK"                              | anviser                                                                                                      |
| OS_ID_FK          | avregningsgrunnlag.oppdragsId      | id på oppdraget i OS                                                                                         |
| OS_LINJE_ID_FK    | avregningsgrunnlag.linjeId         | id på oppdragslinjen i OS                                                                                    |
| TREKKVEDTAK_ID_FK | avregningsgrunnlag.trekkvedtakId   | id på trekk i Skatt- og Trekkomponenten                                                                      |
| GJELDER_ID        | avregningsgrunnlag.gjelderId       | fnr/dnr til rettighetshaver for ytelsen                                                                      |
| FNR_FK            | T_TRANSAKSJON.FNR_FK               | fnr/dnr til personen som mottar ytelsen                                                                      |
| DATO_STATUS       | avregningsgrunnlag.datoStatusSatt  | dato for når returnert status er satt                                                                        |
| STATUS            | avregningsgrunnlag.status          | status fra økonomiløsningen                                                                                  |
| BILAGSNR_SERIE    | avregningsgrunnlag.bilagsnrSerie   | bilagsserie fra UR                                                                                           |
| BILAGSNR          | avregningsgrunnlag.bilagsnr        | bilagsnummer fra UR                                                                                          |
| DATO_FOM          | avregningsgrunnlag.fomdato         | fra-dato i funksjonell periode mottatt av anviser                                                            |
| DATO_TOM          | avregningsgrunnlag.tomdato         | til-dato i funksjonell periode mottatt av anviser                                                            |
| BELOP             | avregningsgrunnlag.belop           | brutto ytelse eller trukket beløp                                                                            |
| DEBET_KREDIT      | avregningsgrunnlag.debetKredit     | fortegn på beløp, D=debet, K=kredit                                                                          |
| UTBETALING_TYPE   | avregningsgrunnlag.utbetalingsType | hvordan utbetalingen er sendt, norsk konto, utenlandsk konto, etc                                            |
| TRANS_TEKST       | avregningsgrunnlag.transTekst      | tekst knyttet til transaksjonen                                                                              |
| TRANS_EKS_ID_FK   | T_TRANSAKSJON.TRANS_EKS_ID_FK      | SPK sin id til transaksjonen<br/>hvis trekk ikke er fra SPK, brukes avregningsgrunnlag.kreditorRef som verdi |
| DATO_AVSENDER     | T_TRANSAKSJON.DATO_ANVISER         | avsenders dato mottatt fra anviser, settes til 1900-01-01 dersom transaksjon ikke er fra SPK                 |
| UTBETALES_TIL     | avregningsgrunnlag.utbetalesTil    | fnr eller orgnr som mottar utbetalingen                                                                      |
| STATUS_TEKST      |                                    | beskrivelse av status                                                                                        |
| RETURTYPE_KODE    |                                    | Ikke i bruk                                                                                                  |
| DUPLIKAT          | "0"                                | Ikke i bruk                                                                                                  |
| TRANSAKSJON_ID    | T_TRANSAKSJON.TRANSAKSJON_ID       | transaksjonens id i T_TRANSAKSJON                                                                            |
| FIL_INFO_INN_ID   |                                    | Ikke i bruk                                                                                                  |
| FIL_INFO_UT_ID    |                                    | filInfoId knyttes til avsendt AVR-fil                                                                        |
| DATO_VALUTERING   | avregningsgrunnlag.datoValutert    | dato når transaksjon er valutert i UR                                                                        |
| KONTO             | avregningsgrunnlag.konto           | regnskapskonto transaksjonen er ført på                                                                      |
| MOT_ID            | avregningsgrunnlag.delytelseId     | transaksjonens motId i T_TRANSAKSJON                                                                         |
| DATO_OPPRETTET    |                                    | opprettelsesdato for record                                                                                  |
| OPPRETTET_AV      | "sokos-spk-mottak"                 | nais appnavn                                                                                                 |
| DATO_ENDRET       |                                    | dato når record er endret                                                                                    |
| ENDRET_AV         | "sokos-spk-mottak"                 | nais appnavn                                                                                                 |
| VERSJON           | 1                                  | ikke i bruk                                                                                                  |


