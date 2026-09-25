# AvstemmingService

Tjenesten er ansvarlig for å sende grensesnittavstemming av utbetalingstransaksjoner til Oppdragssystemet (OS).

**Startbetingelse:**
T_FIL_INFO kolonne `K_AVSTEMMING_S` har status `OSO` (Oppdrag Sendt Ok) og T_TRANSAKSJON har færre enn 500 utbetalingstransaksjoner (tilknyttet en eller flere filinfoIder) som ikke har mottatt
kvitteringsstatus fra OS.

Først hentes alle filinfoIder som har oppfylt følgende krav:

* T_FIL_INFO kolonne `K_AVSTEMMING_S` = `OSO`
* T_TRANSAKSJON har færre enn 500 utbetalingstransaksjoner som ikke har mottatt kvitteringsstatus fra OS

T_FIL_INFO kolonne `K_AVSTEMMING_S` vil bli satt til `OSO` etter at *SendUtbetalingTransaksjonToOppdragZService* har kjørt ferdig for hver filinfoId.

<span style="color: green">FilInfoRepository.getByAvstemmingStatusIsOSO</span><br>

* filInfoId
* antall transaksjoner med mottatt OS-kvittering
* antall transaksjoner uten mottatt OS-kvittering

Spørringen returnerer en map som viser antall transaksjoner med og uten OS-kvittering per filInfoId.

Deretter hentes det opp en liste av *Transaksjonoppsummering* basert på filInfoId.

<span style="color: green">TransaksjonRepository.findTransaksjonOppsummeringByFilInfoId</span><br>
*Transaksjonoppsummering* brukes til å summere opp antall og beløp for transaksjoner som har avstemmingstatus `godkjent`, `varsel`, `avvist` eller `mangler` for hvert fagområde.
*Transaksjonoppsummering* inneholder følgende felter:

* fagområde (_PENSPK_ og _UFORESPK_)
* filInfoId
* osStatus (OS-status)
* transTilstandType (`OSO`, `OSF`, `ORO`, `ORF`)
* antall (antall transaksjoner med OS-status)
* beløp (beløpsum for transaksjoner med OS-status)

Mappingen mellom transaksjonens status og avstemmingstatus:

* osStatus = 0 -> avstemmingstatus = `godkjent`
* osStatus = 1..4 -> avstemmingstatus = `varsel`
* transTilstandType = `ORF` (Oppdrag Retur Feil som betyr at osStatus > 4) -> avstemmingstatus = `avvist`
* osStatus = null og transTilstandType != `OSF` (Oppdrag Sendt Feil) -> avstemmingstatus = `mangler`

*Transaksjonoppsummering* grupperes på fagområdene _PENSPK_ og _UFORESPK_ siden grensesnittavstemmingen utføres per fagområde.

Etter grupperingen henter tjenesten *Transaksjondetalj* per filInfoId.

<span style="color: green">TransaksjonRepository.findTransaksjonDetaljerByFilInfoId</span><br>
*Transaksjondetalj* skal brukes til å rapportere detaljer til `VARS` (Varsling), `AVVI` (Avvist) og `MANG` (Mangler) i grensesnittavstemmingen. *Transaksjondetalj* inneholder følgende felter:

* transaksjonId
* fnr
* fagsystemId
* osStatus (OS-status)
* transTilstandType (`OSO`, `OSF`, `ORO`, `ORF`)
* feilkode (fra OS)
* feilkodeMelding (fra OS)
* datoOpprettet (opprettelse av transaksjon)

For hvert fagområde vil det bli generert en liste av filInfoIder som brukes til å hente *Transaksjondetalj*.

#### Generering av Avstemming XML

For grensesnittavstemming benyttes XML-formaterte meldinger. Hver avstemming består av en sekvens av meldinger: avstemmingStart, avstemmingData (0 eller flere), avstemmingAvsl.

1) Startmelding

| Element | Antall | Datafelt                 | Format | Lengde | Verdi / Kilde   | Kommentar                                             |  
|---------|--------|--------------------------|--------|--------|-----------------|-------------------------------------------------------|
| aksjon  | 1      | aksjonType               | kode   | 8      | START           | Første melding                                        |   
|         |        | kildeType                | kode   | 8      | AVLEV           | Vedtaksløsning skal alltid være avleverende komponent |   
|         |        | avstemmingType           | kode   | 4      | GRSN            | Skal bare lage grensesnittavstemming                  |   
|         |        | avleverendeKomponentKode | kode   | 8      | SPKMOT          | Vedtaksløsningen                                      |   
|         |        | mottakendeKomponentKode  | kode   | 8      | OS              | Oppdragssystemet                                      |   
|         |        | underkomponentKode       | kode   | 8      | PENSPK/UFORESPK | Fagområde                                             |   
|         |        | nokkelFom                | string | 30     | FOM DATO OG TID | Fra første melding som avstemmes (FilInfoId)          |   
|         |        | nokkelTom                | string | 30     | TOM DATO OG TID | Fra siste melding som avstemmes (FilInfoId)           |   
|         |        | tidspunktAvstemmingTom   | string | 26     | Brukes ikke     |                                                       |   
|         |        | avleverendeAvstemmingId  | string | 30     | Unik nøkkel     | Identifiserer denne avstemmingen. Genereres.          |   
|         |        | brukerId                 | string | 8      | MOT             | Systembruker for applikasjon                          |   

2) Datamelding

| Element  | Antall | Datafelt                     | Obligatorisk | Format      | Lengde | Verdi / Kilde                                                          | Kommentar                                        |
|----------|--------|------------------------------|--------------|-------------|--------|------------------------------------------------------------------------|--------------------------------------------------|
| aksjon   | 1      | aksjonType                   | Ja           | kode        | 8      | DATA                                                                   |                                                  |
|          |        | øvrige felter                |              |             |        |                                                                        |                                                  |
| total    | 1      | totalAntall                  | Ja           | int         |        | Totalt antall oppdragsmeldinger sendt i det intervallet man melder om. |                                                  |
|          |        | totalBelop                   | Nei          | xsd:decimal |        | Sum av alle oppdrag innen tidsintervall                                |                                                  | 
|          |        | fortegn                      | Nei          | string      | 1      | T for tillegg, F for fradrag                                           |                                                  |
| periode  | 1      | datoAvstemtFom               | Ja           | ååååmmddhh  | 10     | Startdato for avstemmingen                                             |                                                  |
|          |        | datoAvstemtTom               | Ja           | ååååmmddhh  | 10     | Sluttdato for avstemmingen                                             |                                                  |
| grunnlag | 1      | godkjentAntall               | Ja           | int         |        | Antall godkjente meldinger                                             |                                                  |
|          |        | godkjentBelop                | Nei          | xsd:decimal |        | Sumbeløp på disse                                                      |                                                  |
|          |        | godkjentFortegn              | Nei          | string      | 1      | T/F                                                                    |                                                  |
|          |        | varselAntall                 | Ja           | int         |        | Antall meldinger med varsel                                            |                                                  |
|          |        | varselBelop                  | Nei          | xsd:decimal |        | Sumbeløp på disse                                                      |                                                  |
|          |        | varselFortegn                | Nei          | string      | 1      | T/F                                                                    |                                                  |
|          |        | avvistAntall                 | Ja           | int         |        | Antall avviste meldinger                                               |                                                  |
|          |        | avvistBelop                  | Nei          | xsd:decimal |        | Sumbeløp på disse                                                      |                                                  |
|          |        | avvistFortegn                | Nei          | string      | 1      | T/F                                                                    |                                                  |
|          |        | manglerAntall                | Ja           | int         |        | Antall meldinger som mangler kvittering                                |                                                  |
|          |        | manglerBelop                 | Nei          | xsd:decimal |        | Sumbeløp på disse                                                      |                                                  |
|          |        | manglerFortegn               | Nei          | string      | 1      | T/F                                                                    |                                                  |
| detalj   | 0..N   | detaljType                   | Ja           | string      | 4      | VARS, AVVI, MANG                                                       |                                                  |
|          |        | offnr                        | Ja           | string      | 11     | TRANSAKSJONDETALJ.FNR                                                  | fødselsnummer for bruker detaljmeldingen gjelder |
|          |        | avleverendeTransaksjonNokkel | Ja           | string      | 30     | TRANSAKSJONDETALJ.FAGSYSTEM_ID                                         | Avleverende systems identifikasjon av vedtaket   |
|          |        | meldingKode                  | Nei          | kode        | 8      | TRANSAKSJONDETALJ.FEILKODE                                             | Fra mottatt kvittering                           |
|          |        | alvorlighetsgrad             | Nei          | string      | 2      | TRANSAKSJONDETALJ.OS_STATUS                                            | Fra mottatt kvittering                           |
|          |        | tekstMelding                 | Nei          | string      | 70     | TRANSAKSJONDETALJ.FEILKODE_MELDING                                     | Fra mottatt kvittering                           |
|          |        | tidspunkt                    | Ja           | string      | 26     | TRANSAKSJONDETALJ.DATO_OPPRETTET                                       | Format åååå-mm-dd-hh.mm.ss.nnnnnn                |                                                                                          |

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
        <avleverendeKomponentKode>SPKMOT</avleverendeKomponentKode>
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
        <avleverendeKomponentKode>SPKMOT</avleverendeKomponentKode>
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
<ns2:avstemmingsdata xmlns:ns2="http://nav.no/virksomhet/tjenester/avstemming/meldinger/v1">
    <aksjon>
        <aksjonType>DATA</aksjonType>
        <kildeType>AVLEV</kildeType>
        <avstemmingType>GRSN</avstemmingType>
        <avleverendeKomponentKode>SPKMOT</avleverendeKomponentKode>
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
        <avleverendeKomponentKode>SPKMOT</avleverendeKomponentKode>
        <mottakendeKomponentKode>OS</mottakendeKomponentKode>
        <underkomponentKode>PENSPK</underkomponentKode>
        <nokkelFom>17301</nokkelFom>
        <nokkelTom>17302</nokkelTom>
        <avleverendeAvstemmingId>333256da-560a-45da-ba27-d781b7</avleverendeAvstemmingId>
        <brukerId>MOT</brukerId>
    </aksjon>
</avstemmingsdata>
```