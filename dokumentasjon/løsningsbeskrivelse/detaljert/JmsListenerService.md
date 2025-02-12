# JmsListenerService

Tjenesten lytter til

* kvitteringsmeldinger sendt fra OS og lagrer nødvendig meldingsinformasjon i T_TRANSAKSJON og T_TRANS_TILSTAND. Kvitteringsmeldinger inneholder informasjon om status til behandlingen av
  transaksjonene (utbetaling- og trekktransaksjoner) i OS.
* meldinger brukt som grunnlag for avregninger sendt fra UR. Disse medlingene behandles, lagres og utgjør avregningsinformasjon som oversendes SPK.

**Kvitteringsmeldinger:**
For kvitteringsmeldinger sjekkes statusinformasjonen (**alvorlighetsgrad**) og dersom

* alvorlighetsgrad > 4, logges feilen og transaksjonen lagres med status **ORF** (Oppdrag Retur Feil hvis utbetalingsmelding) eller **TRF** (Trekk Retur Feil hvis trekkmelding) i T_TRANSAKSJON og
  T_TRANS_TILSTAND
* alvorlighetsgrad < 4 lagres status **ORO** (Oppdrag Retur Ok hvis utbetalingsmelding) eller **TRO** (Trekk Retur Ok hvis trekkmelding) i T_TRANSAKSJON og T_TRANS_TILSTAND

Videre sjekkes det om kvitteringsmeldingen er en duplikatmelding. Duplikatsjekken gjøres på **transaksjonsId** for trekkmeldinger og **delytelseId** for utbetalingsmeldinger.

1. Dersom alvorlighetsgrad = "00" (status ok) bypasses duplikatsjekken og status **ORO** eller **TRO** lagres.
2. Dersom alvorlighetsgrad != "00" og transaksjonen eksisterer i T_TRANSAKSJON med status **ORO** eller **TRO**, blir ikke transaksjonens status oppdatert.
3. Dersom hverken 1 eller 2, blir transaksjonens status oppdatert.

I T_TRANS_TILSTAND blir det opprettet en ny rad hvor kolonnene **feilkode** og **feilkodemelding** tildeles verdi fra henholdsvis **kodeMelding** og **beskrMelding** fra OS-meldingen.
I T_TRANSAKSJON blir kolonnen **os_status** oppdatert med **alvorlighetsgrad** fra OS-meldingen, og dersom det er en trekkmelding blir i tillegg kolonnen **trekkvedtak_id_fk** oppdatert fra
OS-meldingen.

**Avregningsmeldinger:**
Avregningsmeldingene inneholder grunnlagsinformasjon fra UR og brukes til å danne avregningsinformasjon som oversendes SPK daglig som filer.
Dersom avregningsmeldingen inneholder delYtelseId (= utbetalingsmeldingens mottaksId) gjøre det et oppslag i T_TRANSAKSJON med denne parameteren som nøkkel i tillegg til tomDato (utbetalingperiodens
til-dato). Historisk har mottaksId vært benyttet for flere enn en transaksjon og følgelig er tomDato en del av nøkkelen).
Dagens løsning har derimot bare en mottaksId per transaksjon.
Hvis avregningsmeldingen ikke er knyttet til en utbetalingsmelding, men derimot en trekkmelding, vil parameteren trekkVedtakId brukes som nøkkel for knytting til transaksjonen i T_TRANSAKSJON.
Følgende informasjonen hentes fra T_TRANSAKSJON og knyttes til avregningstransaksjonen:

* fnrFk (personens fødselsnummer)
* transEksId (transaksjonsid til SPK)
* datoAnviser (SPK sin anvisningsdato)
* transaksjonId (transaksjonens id)

Hvis en avregningsmelding som er knyttet til en trekkmelding ikke finnes i T_TRANSAKSJON, vil det utføres et rest-kall mot OS med parameteren trekkVedtakId for å finne en referanse til en kreditor.
Dersom det finnes en kreditor, vil transEksId (SPK sin transaksjonsid) tildeles denne kreditorreferansen.

Avregningsmeldingen blir mappet og persistert til en rad i T_RETUR_TIL_ANV.

**Format på avregningsmelding:**
<br/>&emsp;avregningsgrunnlag.returType (String(1))
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

Eksempel:

```json
{
   "avregningsgrunnlag":{
      "returType":"U",
      "oppdragsId":70014840,  
      "linjeId":3,
      "trekkvedtakId":0, 
      "gjelderId":"03117232831",
      "utbetalesTil":"03117232831",
      "datoStatusSatt":"20240219", 
      "status":"0018",
      "bilagsnrSerie":"10",   
      "bilagsnr":"759197901",  
      "konto":"008404500",   
      "fomdato":"20240201",
      "tomdato":"20240229",
      "belop":5811,
      "debetKredit":"D",
      "utbetalingsType":"BK1",  
      "transTekst":"0030 012924639",  
      "datoValutert":"20240219",
      "delytelseId":"84004200"  
   }
}
```

**Mapping T_RETUR_TIL_ANV**

| Kolonnenavn       | Verdi                                 | Kommentar                                 |
|-------------------|---------------------------------------|-------------------------------------------|
| RETUR_TIL_ANV_ID  |                                       | settes av databasen                       |
| RECTYPE           | "02"                                  | recordtype                                |
| K_RETUR_T         | "AVR"                                 | avregningstype                            |
| K_ANVISER         | "SPK"                                 | anviser                                   |
| OS_ID_FK          | avregningsgrunnlag.oppdragsId         | id på oppdraget i OS                      |
| OS_LINJE_ID_FK    | avregningsgrunnlag.linjeId            | id på oppdragslinjen i OS                 |
| TREKKVEDTAK_ID_FK | avregningsgrunnlag.trekkvedtakId      | id av trekk i Skatt- og Trekkomponenten   |
| GJELDER_ID        | avregningsgrunnlag.gjelderId          | fnr/dnr til rettighetshaver for ytelsen   |
| FNR_FK            | T_TRANSAKSJON.FNR_FK                  | fnr/dnr til personen som mottar ytelsen   |
| DATO_STATUS       | avregningsgrunnlag.datoStatusSatt     | dato for når returnert status er satt     |
| STATUS            | avregningsgrunnlag.status             | status fra økonomiløsningen               |
| BILAGSNR_SERIE    | avregningsgrunnlag.bilagsnrSerie      | bilagsserie fra UR                        |
| BILAGSNR          | avregningsgrunnlag.bilagsnr           | bilagsnummer fra UR                       |
| DATO_FOM          | avregningsgrunnlag.fomdato            | funksjonell periode mottatt av anviser    |
| DATO_TOM          | avregningsgrunnlag.tomdato            | funksjonell periode mottatt av anviser    |
| BELOP             | avregningsgrunnlag.belop              | brutto ytelse eller trukket beløp         |
| DEBET_KREDIT      | avregningsgrunnlag.debetKredit        | fortegn på beløp, D=debet, K=kredit       |
| UTBETALING_TYPE   | avregningsgrunnlag.utbetalingsType    | hvordan utbetalingen er sendt             |
| TRANS_TEKST       | avregningsgrunnlag.transTekst         | tekst knyttet til transaksjonen           |
| TRANS_EKS_ID_FK   | T_TRANSAKSJON.TRANS_EKS_ID_FK         | SPK sin id til transaksjonen              |
| DATO_AVSENDER     | T_TRANSAKSJON.DATO_ANVISER            | avsenders dato mottatt fra anviser        |
| UTBETALES_TIL     | avregningsgrunnlag.utbetalesTil       | fnr eller orgnr som mottar utbetalingen   |
| STATUS_TEKST      |                                       | beskrivelse av status                     |
| RETURTYPE_KODE    | avregningsgrunnlag.returType          |                                           |
| DUPLIKAT          | "0"                                   | Ikke i bruk                               |
| TRANSAKSJON_ID    | T_TRANSAKSJON.TRANSAKSJON_ID          | transaksjonens id i T_TRANSAKSJON         |
| FIL_INFO_INN_ID   |                                       | Ikke i bruk                               |
| FIL_INFO_UT_ID    |                                       | filInfoId knyttes til avsendt AVR-fil     |
| DATO_VALUTERING   | avregningsgrunnlag.datoValutert       | dato når transaksjon er valutert i UR     |
| KONTO             | avregningsgrunnlag.konto              | regnskapskonto transaksjonen er ført på   |
| MOT_ID            | avregningsgrunnlag.delytelseId        | transaksjonens motId i T_TRANSAKSJON      |
| DATO_OPPRETTET    |                                       | opprettelsesdato for record               |
| OPPRETTET_AV      | "sokos-spk-mottak"                    | nais appnavn                              |
| DATO_ENDRET       |                                       | dato når record er endret                 |
| ENDRET_AV         | "sokos-spk-mottak"                    | nais appnavn                              |
| VERSJON           | 1                                     | ikke i bruk                               |


