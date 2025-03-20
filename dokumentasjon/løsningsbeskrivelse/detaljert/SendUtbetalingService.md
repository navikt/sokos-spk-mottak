# SendUtbetalingService
Tjenesten er ansvarlig for å sende alle utbetalingstransaksjoner til Oppdragssystemet (OS) som ligger i T_TRANSAKSJON og som ikke er oversendt ennå eller har feilet ved tidligere oversendelsesforsøk.

**Startbetingelse:** T_INN_TRANSAKSJON er tom.

Først hentes alle transaksjoner som har en av følgende statuser: 
* opprettet (OPR)
* feil ved sending (OSF)
* manuelt korrigert (MKR)

Transaksjonene grupperes på personId og fagområde før de konverteres til utbetalingsmeldinger. Dette fører til at transaksjoner knyttet til samme person og fagomåde kommer i samme melding. 
Deretter oversendes meldingene batchvis til en MQ-kø på xml-format.

For hver meldingsbatch-operasjon mot kø blir T_TRANSAKSJON og T_TRANS_TILSTAND oppdatert med riktig status for de transaksjoner som inngår i meldingene.
Dersom db-operasjonene skulle feile, vil status til transaksjonene i T_TRANSAKSJON og T_TRANS_TILSTAND forbli uforandret, dvs opprettet (OPR), og følgelig vil de oversendes til kø neste gang tjenesten kjører.
Dette kan resulterer i duplikater i OS, men dette vil bli håndtert i mottaket av reply-meldinger fra OS.

Løsningen unngår derfor den uønskede tilstanden at db viser ok-status mens meldingene ikke har blitt sendt suksessfult til køen.

Når meldingsoversendelsen er ferdig, oppdateres T_FIL_INFO med K_AVSTEMMING_S lik OSO (Oppdrag Sendt Ok) for de anvisningsfilene som inneholdt de ferdigprosesserte utbetalingstransaksjonene.

**Mapping av utbetalingsmeldinger**

oppdrag.oppdrag-110.kodeAksjon = "1"
<br/>oppdrag.oppdrag-110.kodeEndring = "NY" (hvis transTolkning = "NY"), "END" (hvis transTolkning = "NY_EKSIST" og fnrEndret == "1") ellers "UEND"
<br/>oppdrag.oppdrag-110.kodeFagomraade = T_K_GYLDIG_KOMBIN.K_FAGOMRAADE
<br/>oppdrag.oppdrag-110.fagsystemId = T_TRANSAKSJON.person_id
<br/>oppdrag.oppdrag-110.utbetFrekvens = "MND"
<br/>oppdrag.oppdrag-110.stonadId = T_TRANSAKSJON.dato_fom
<br/>oppdrag.oppdrag-110.oppdragGjelderId = T_TRANSAKSJON.fnr_fk
<br/>oppdrag.oppdrag-110.datoOppdragGjelderFom = 1900-01-01
<br/>oppdrag.oppdrag-110.saksbehId = "MOT"
<br/>&emsp;oppdrag.oppdrag-110.avstemming-115.kodeKomponent = "SPKMOT"
<br/>&emsp;oppdrag.oppdrag-110.avstemming-115.nokkelAvstemming = T_TRANSAKSJON.fil_info_id
<br/>&emsp;oppdrag.oppdrag-110.avstemming-115.tidspktMelding = T_TRANSAKSJON.dato_opprettet
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-enhet-120.typeEnhet = "BOS" (hvis transTolkning = "NY")
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-enhet-120.enhet = "4819" (hvis transTolkning = "NY")
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-enhet-120.datoEnhetFom = 1900-01-01 (hvis transTolkning = "NY")
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.kodeEndringLinje = "NY"
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.delytelseId = T_TRANSAKSJON.mot_id
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.kodeKlassifik = T_K_GYLDIG_KOMBIN.os_klassifikasjon
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.datoKlassifikFom = 1900-01-01
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.datoVedtakFom = T_TRANSAKSJON.dato_fom
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.datoVedtakTom = T_TRANSAKSJON.dato_tom
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.sats = T_TRANSAKSJON.belop/100
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.fradragTillegg = "T"
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.typeSats = "MND"
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.skyldnerId = "80000427901"
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.brukKjoreplan = "N"
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.saksbehId = "MOT"
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.utbetalesTilId = T_TRANSAKSJON.fnr_fk
<br/>&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.typeSoknad = "EO" (hvis art = "UFE")
<br/>&emsp;&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.attestant-180.attestantId = "MOT"
<br/>&emsp;&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.grad-170.typeGrad = "UTAP", "AFPG", "UBGR" eller "UFOR" (avhengig av art)
<br/>&emsp;&emsp;oppdrag.oppdrag-110.oppdrags-linje-150.grad-170.grad = T_TRANSAKSJON.grad

Eksempel:

```xml
<?xml version="1.0" encoding="utf-8"?>
<oppdrag xmlns="http://www.trygdeetaten.no/skjema/oppdrag">
    <oppdrag-110>
        <kodeAksjon>1</kodeAksjon>
        <kodeEndring>NY</kodeEndring>
        <kodeFagomraade>PENSPK</kodeFagomraade>
        <fagsystemId>600079</fagsystemId>
        <utbetFrekvens>MND</utbetFrekvens>
        <stonadId>20240501</stonadId>
        <oppdragGjelderId>12345678901</oppdragGjelderId>
        <datoOppdragGjelderFom>1900-01-01</datoOppdragGjelderFom>
        <saksbehId>MOT</saksbehId>
        <avstemming-115>
            <kodeKomponent>SPKMOT</kodeKomponent>
            <nokkelAvstemming>20039237</nokkelAvstemming>
            <tidspktMelding>2024-08-21T16:13:14.641291</tidspktMelding>
        </avstemming-115>
        <oppdrags-enhet-120>
            <typeEnhet>BOS</typeEnhet>
            <enhet>4819</enhet>
            <datoEnhetFom>1900-01-01</datoEnhetFom>
        </oppdrags-enhet-120>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>20025925</delytelseId>
            <kodeKlassifik>PENSPKALD01</kodeKlassifik>
            <datoKlassifikFom>1900-01-01</datoKlassifikFom>
            <datoVedtakFom>2024-05-01</datoVedtakFom>
            <datoVedtakTom>2024-05-31</datoVedtakTom>
            <sats>3055</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>MND</typeSats>
            <skyldnerId>12345678901</skyldnerId>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>MOT</saksbehId>
            <utbetalesTilId>12345678901</utbetalesTilId>
            <attestant-180>
                <attestantId>MOT</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
        <oppdrags-linje-150>
            <kodeEndringLinje>NY</kodeEndringLinje>
            <delytelseId>20025926</delytelseId>
            <kodeKlassifik>PENSPKALD-OP</kodeKlassifik>
            <datoKlassifikFom>1900-01-01</datoKlassifikFom>
            <datoVedtakFom>2024-05-01</datoVedtakFom>
            <datoVedtakTom>2024-05-31</datoVedtakTom>
            <sats>3055</sats>
            <fradragTillegg>T</fradragTillegg>
            <typeSats>MND</typeSats>
            <skyldnerId>12345678901</skyldnerId>
            <brukKjoreplan>N</brukKjoreplan>
            <saksbehId>MOT</saksbehId>
            <utbetalesTilId>12345678901</utbetalesTilId>
            <attestant-180>
                <attestantId>MOT</attestantId>
            </attestant-180>
        </oppdrags-linje-150>
    </oppdrag-110>
</oppdrag>