# AvstemmingService

Tjenesten er ansvarlig for å sende grensesnittavstemming av utbetalingstransaksjoner til Oppdragssystemet (OS).

**Startbetingelse:** T_FIL_INFO `K_AVSTEMMING_S` har status `OSO` (Oppdrag Sendt Ok) og T_TRANSAKSJON har under 500 utbetalingstransaksjoner som ikke har fått `OSO` (Oppdrag Sendt Ok) for en eller
flere filinfoIder.

Først hentes alle filinfo som har oppfylt følgende krav:

* T_FIL_INFO `K_AVSTEMMING_S` = `OSO`
* T_TRANSAKSJON under 500 utbetalingstransaksjoner med `T_TRANS_TILST_T` = `OSO`

T_FIL_INFO sin K_AVSTEMMING_S vil bli satt til `OSO` etter at *SendUtbetalingTransaksjonToOppdragZService* er kjørt ferdig for hver filinfoId. T_TRANSAKSJON under 500 utbetalingstransaksjoner brukes
for å sikre at applikasjonen har nok tid til å motta kvitteringer gjennom *JmsListenerService*.

<span style="color: green">FilInfoRepository.getByAvstemmingStatusIsOSO</span><br>

* filInfoId
* antall transaksjoner

Dette skal returnere en Map som viser antall transaksjoner per filInfoId. Deretter vil det hente opp en liste av *Transaksjonoppsummering* basert på filInfoId.

<span style="color: green">TransaksjonRepository.findTransaksjonOppsummeringByFilInfoId</span><br>
*Transaksjonoppsummering* skal brukes til å summere opp antall og beløp for `godkjent`, `varsel`, `avvist` og `mangler` transaksjoner for hvert fagområde. *Transaksjonoppsummering* er en modell som
inneholder følgende felter:

* fagområde
* filInfoId
* osStatus (Oppdrag status)
* transTilstandType (OSO, ORF, ORO)
* antall (antall transaksjoner for osStatus)
* beløp (summen av beløp for osStatus)

*Transaksjonoppsummering* skal grupperes på fagområdene PENSPK og UFORESPK. Når det foretas grensesnittavstemming må det skilles med fagområde per avstemming. Etter gruppering trenger tjenesten å
hente ut *Transaksjondetalj* per filInfoId.

<span style="color: green">TransaksjonRepository.findTransaksjonDetaljerByFilInfoId</span><br>
*Transaksjondetalj* skal brukes til å rapportere detaljer til `VARS` (Varsling), `AVVI` (Avvik) og `MANG` (Mangler) i grensesnittavstemming. *Transaksjondetalj* er en modell som inneholder følgende
felter:

* transaksjonId
* fnr (fødselsnummer)
* fagsystemId
* osStatus (Oppdrag status)
* transTilstandType (OSO, ORF, ORO)
* feilkode (Feilkode fra Oppdrag)
* feilkodeMelding (Feilmelding fra Oppdrag)
* datoOpprettet (tidspunkt for opprettelse av transaksjon)

For hvert fagområde vil det bli en liste av filInfoIder som brukes til å hente *Transaksjondetalj*.

#### Generering av Avstemming XML

For grensesnittavstemming benyttes XML-formaterte meldinger. Hver avstemming består av en sekvens av meldinger: avstemmingStart, avstemmingData (0 eller flere), avstemmingAvsl.

1) Startmelding

| Element | Antall | Datafelt                 | Format | Lengde | Verdi / Kilde   | Kommentar                                             |  
|---------|--------|--------------------------|--------|--------|-----------------|-------------------------------------------------------|
| aksjon  | 1      | aksjonType               | kode   | 8      | START           | Første melding                                        |   
|         |        | kildeType                | kode   | 8      | AVLEV           | Vedtaksløsning skal alltid være avleverende komponent |   
|         |        | avstemmingType           | kode   | 4      | GRSN            | Skal bare lage grensesnittavstemming                  |   
|         |        | avleverendeKomponentKode | kode   | 8      | MOTTKOMP        | Vedtaksløsningen                                      |   
|         |        | mottakendeKomponentKode  | kode   | 8      | OS              | Oppdragssystemet                                      |   
|         |        | underkomponentKode       | kode   | 8      | PENSPK/UFORESPK | Fagområde                                             |   
|         |        | nokkelFom                | string | 30     | FOM DATO OG TID | Fra første melding som avstemmes (FilInfoId)          |   
|         |        | nokkelTom                | string | 30     | TOM DATO OG TID | Fra siste melding som avstemmes (FilInfoId)           |   
|         |        | tidspunktAvstemmingTom   | string | 26     | Brukes ikke     |                                                       |   
|         |        | avleverendeAvstemmingId  | string | 30     | Unik nøkkel     | Identifiserer denne avstemmingen. Genereres.          |   
|         |        | brukerId                 | string | 8      | MOT             | Systembruker for applikasjon                          |   

2) Datamelding

| Element  | Antall  | Datafelt                     | Obligatorisk | Format      | Lengde | Verdi / Kilde                                                          | Kommentar                                        |
|----------|---------|------------------------------|--------------|-------------|--------|------------------------------------------------------------------------|--------------------------------------------------|
| aksjon   | 1       | aksjonType                   | Ja           | kode        | 8      | DATA                                                                   |                                                  |
|          |         | øvrige felter                |              |             |        |                                                                        |                                                  |
| total    | 1       | totalAntall                  | Ja           | int         |        | Totalt antall oppdragsmeldinger sendt i det intervallet man melder om. |                                                  |
|          |         | totalBelop                   | Nei          | xsd:decimal |        | Sum av alle oppdrag innen tidsintervall                                |                                                  | 
|          |         | fortegn                      | Nei          | string      | 1      | T for tillegg, F for fradrag                                           |                                                  |
| periode  | 1       | datoAvstemtFom               | Ja           | ååååmmddhh  | 10     | Startdato for avstemmingen                                             |                                                  |
|          |         | datoAvstemtTom               | Ja           | ååååmmddhh  | 10     | Sluttdato for avstemmingen                                             |                                                  |
| grunnlag | 1       | godkjentAntall               | Ja           | int         |        | Antall godkjente meldinger                                             |                                                  |
|          |         | godkjentBelop                | Nei          | xsd:decimal |        | Sumbeløp på disse                                                      |                                                  |
|          |         | godkjentFortegn              | Nei          | string      | 1      | T/F                                                                    |                                                  |
|          |         | varselAntall                 | Ja           | int         |        | Antall meldinger med varsel                                            |                                                  |
|          |         | varselBelop                  | Nei          | xsd:decimal |        | Sumbeløp på disse                                                      |                                                  |
|          |         | varselFortegn                | Nei          | string      | 1      | T/F                                                                    |                                                  |
|          |         | avvistAntall                 | Ja           | int         |        | Antall avviste meldinger                                               |                                                  |
|          |         | avvistBelop                  | Nei          | xsd:decimal |        | Sumbeløp på disse                                                      |                                                  |
|          |         | avvistFortegn                | Nei          | string      | 1      | T/F                                                                    |                                                  |
|          |         | manglerAntall                | Ja           | int         |        | Antall meldinger som mangler kvittering                                |                                                  |
|          |         | manglerBelop                 | Nei          | xsd:decimal |        | Sumbeløp på disse                                                      |                                                  |
|          |         | manglerFortegn               | Nei          | string      | 1      | T/F                                                                    |                                                  |
| detalj   | 0..N *) | detaljType                   | Ja           | string      | 4      | VARS, AVVI, MANG                                                       |                                                  |
|          |         | offnr                        | Ja           | string      | 11     | TRANSAKSJONDETALJ.FNR                                                  | fødselsnummer for bruker detaljmeldingen gjelder |
|          |         | avleverendeTransaksjonNokkel | Ja           | string      | 30     | TRANSAKSJONDETALJ.FAGSYSTEM_ID                                         | Avleverende systems identifikasjon av vedtaket   |
|          |         | meldingKode                  | Nei          | kode        | 8      | TRANSAKSJONDETALJ.FEILKODE                                             | Fra mottatt kvittering **)                       |
|          |         | alvorlighetsgrad             | Nei          | string      | 2      | TRANSAKSJONDETALJ.OS_STATUS                                            | Fra mottatt kvittering **)                       |
|          |         | tekstMelding                 | Nei          | string      | 70     | TRANSAKSJONDETALJ.FEILKODE_MELDING                                     | Fra mottatt kvittering **)                       |
|          |         | tidspunkt                    | Ja           | string      | 26     | TRANSAKSJONDETALJ.DATO_OPPRETTET                                       | Format åååå-mm-dd-hh.mm.ss.nnnnnn                |                                                                                          |

3) Sluttmelding

| Element | Antall | Datafelt      | Format | Lengde | Verdi / Kilde | Kommentar                          |
|---------|--------|---------------|--------|--------|---------------|------------------------------------|
| aksjon  | 1      | aksjonType    | kode   | 8      | AVSL          |                                    |
|         |        | øvrige felter |        |        |               | Samme innhold som for Startmelding |

Eksempel:

1) Startmelding

```xml
<?xml version='1.0' encoding='UTF-8'?>
<avstemmingsdata>
    <aksjon>
        <aksjonType>START</aksjonType>
        <kildeType>AVLEV</kildeType>
        <avstemmingType>GRSN</avstemmingType>
        <avleverendeKomponentKode>MOTTKOMP</avleverendeKomponentKode>
        <mottakendeKomponentKode>OS</mottakendeKomponentKode>
        <underkomponentKode>PENSPK</underkomponentKode>
        <nokkelFom>17301</nokkelFom>
        <nokkelTom>17302</nokkelTom>
        <avleverendeAvstemmingId>333256da-560a-45da-ba27-d781b7</avleverendeAvstemmingId>
        <brukerId>MOT</brukerId>
    </aksjon>
</avstemmingsdata>
```

2) Datamelding 

*Grunnlag*
```xml
<?xml version='1.0' encoding='UTF-8'?>
<avstemmingsdata>
    <aksjon>
        <aksjonType>DATA</aksjonType>
        <kildeType>AVLEV</kildeType>
        <avstemmingType>GRSN</avstemmingType>
        <avleverendeKomponentKode>MOTTKOMP</avleverendeKomponentKode>
        <mottakendeKomponentKode>OS</mottakendeKomponentKode>
        <underkomponentKode>PENSPK</underkomponentKode>
        <nokkelFom>17301</nokkelFom>
        <nokkelTom>17302</nokkelTom>
        <avleverendeAvstemmingId>333256da-560a-45da-ba27-d781b7</avleverendeAvstemmingId>
        <brukerId>MOT</brukerId>
    </aksjon>
    <total>
        <totalAntall>0</totalAntall>
        <totalBelop>0</totalBelop>
        <fortegn>T</fortegn>
    </total>
    <periode>
        <datoAvstemtFom>2024102900</datoAvstemtFom>
        <datoAvstemtTom>2024102923</datoAvstemtTom>
    </periode>
    <grunnlag>
        <godkjentAntall>0</godkjentAntall>
        <godkjentBelop>0</godkjentBelop>
        <godkjentFortegn>T</godkjentFortegn>
        <varselAntall>0</varselAntall>
        <varselBelop>0</varselBelop>
        <varselFortegn>T</varselFortegn>
        <avvistAntall>0</avvistAntall>
        <avvistBelop>0</avvistBelop>
        <avvistFortegn>T</avvistFortegn>
        <manglerAntall>0</manglerAntall>
        <manglerBelop>0</manglerBelop>
        <manglerFortegn>T</manglerFortegn>
    </grunnlag>
</avstemmingsdata>
```

*Detalj*
```xml
<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<ns2:avstemmingsdata xmlns:ns2="http://nav.no/virksomhet/tjenester/avstemming/me
ldinger/v1">
    <aksjon>
        <aksjonType>DATA</aksjonType>
        <kildeType>AVLEV</kildeType>
        <avstemmingType>GRSN</avstemmingType>
        <avleverendeKomponentKode>MOTTKOMP</avleverendeKomponentKode>
        <mottakendeKomponentKode>OS</mottakendeKomponentKode>
        <underkomponentKode>PENSPK</underkomponentKode>
        <nokkelFom>17301</nokkelFom>
        <nokkelTom>17302</nokkelTom>
        <avleverendeAvstemmingId>333256da-560a-45da-ba27-d781b7</avleverendeAvstemmingId>
        <brukerId>MOT</brukerId>
    </aksjon>
    <total>
        <totalAntall>8</totalAntall>
        <totalBelop>4919</totalBelop>
        <fortegn>T</fortegn>
    </total>
    <periode>
        <datoAvstemtFom>2024102909</datoAvstemtFom>
        <datoAvstemtTom>2024102915</datoAvstemtTom>
    </periode>
    <grunnlag>
        <godkjentAntall>7</godkjentAntall>
        <godkjentBelop>4634</godkjentBelop>
        <godkjentFortegn>T</godkjentFortegn>
        <varselAntall>0</varselAntall>
        <varselBelop>0</varselBelop>
        <varselFortegn>T</varselFortegn>
        <avvistAntall>1</avvistAntall>
        <avvistBelop>285</avvistBelop>
        <avvistFortegn>T</avvistFortegn>
        <manglerAntall>0</manglerAntall>
        <manglerBelop>0</manglerBelop>
        <manglerFortegn>T</manglerFortegn>
    </grunnlag>
    <detalj>
        <detaljType>AVVI</detaljType>
        <offnr>20486818310</offnr>
        <avleverendeTransaksjonNokkel>202410291002</avleverendeTransaksjonNokkel>
        <meldingKode>B110034F</meldingKode>
        <alvorlighetsgrad>08</alvorlighetsgrad>
        <tekstMelding>Mangler planlagt kj.replan p. oppgitt frekvens</tekstMelding>
        <tidspunkt>2024-10-29-09.54.31.301824</tidspunkt>
    </detalj>
</ns2:avstemmingsdata>
```

3) Sluttmelding

```xml
<?xml version='1.0' encoding='UTF-8'?>
<avstemmingsdata>
    <aksjon>
        <aksjonType>AVSL</aksjonType>
        <kildeType>AVLEV</kildeType>
        <avstemmingType>GRSN</avstemmingType>
        <avleverendeKomponentKode>MOTTKOMP</avleverendeKomponentKode>
        <mottakendeKomponentKode>OS</mottakendeKomponentKode>
        <underkomponentKode>PENSPK</underkomponentKode>
        <nokkelFom>17301</nokkelFom>
        <nokkelTom>17302</nokkelTom>
        <avleverendeAvstemmingId>333256da-560a-45da-ba27-d781b7</avleverendeAvstemmingId>
        <brukerId>MOT</brukerId>
    </aksjon>
</avstemmingsdata>
```