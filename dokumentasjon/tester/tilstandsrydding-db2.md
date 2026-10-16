# DBA-test: rydding av transaksjonstilstander

**Status: NO-GO. Testene er ikke gjennomført.**

Tillatelse til en begrenset produksjonsprøve krever at sikkerhetstestene nedenfor er bestått på Db2 z/OS,
med dokumentert evidens og godkjenning fra DBA og applikasjonsansvarlig.
Manglende test, målegrense eller evidens gir NO-GO. H2-tester erstatter ikke
Db2-testene. Akseptabel last på delt maskinvare avgjøres deretter med
[produksjonsprotokollen](../drift/tilstandsrydding-produksjonsprove.md), ikke som
et krav om full lastvalidering i testmiljø. Cron og funksjonsbryter kommer i en senere PR.

## Testgrunnlag og grenser

Registrer dato, ansvarlige, miljø/skjema, Db2-/driverversjon, kodeversjon og
konfigurasjon. Beskriv datavolum, alder/referansefordeling, statistikk,
isolasjon og LOCKSIZE/LOCKMAX. Arkiver SQL, planer og målinger på avtalt sted.

Bruk et kontrollert testmiljø med representative data og samtidig ordinær
trafikk. Avtal stoppansvarlig, stoppmetode og gjenoppretting. Feil-/krasjtester
skal ikke kjøres i produksjon. Bruk syntetiske data og ikke lagre persondata
i evidensen.

DBA og berørte tjenesteeiere fyller inn konkrete grenser før testing.
Startverdiene i applikasjonen er ikke godkjente lastgrenser.

| Måling | Enhet/målevindu | Baseline uten rydding | Absolutt grense / tillatt endring | Målekilde/ansvarlig |
| --- | --- | --- | --- | --- |
| Låseventing og deadlock-frekvens | Ikke avtalt | Ikke målt | Ikke avtalt | Ikke registrert |
| SQL-, poolvente-, commit-, rollback- og stopptid | Ikke avtalt | Ikke målt | Ikke avtalt | Ikke registrert |

Registrer separate grenser for hver måling i tabellen. Stopp ved første
overskridelse; gjennomsnittsverdier skal ikke skjule påvirkning på andre tjenester.

## Tester

DBA leder T01 og T02 sammen med applikasjonsansvarlig.
Kjør i rekkefølge; muterende tester forutsetter godkjente tilgangsstier.
For hvert tilfelle registreres utfører/dato, parametere, faktisk resultat,
evidens og avvik i resultat-tabellen.

`ZOS.TXT` er en produksjonsdump og legges til grunn for indekser og
fremmednøkler. Schemaet endres ikke; en separat katalogkontroll inngår derfor
ikke i testprotokollen. EXPLAIN og låsetestene nedenfor beholdes fordi de
kontrollerer tilgangsstier og kjøreatferd, ikke om schemaet samsvarer med dumpen.

### T01: EXPLAIN og SQL-kostnad

SQL-en nedenfor viser setningene som applikasjonen bruker med seksmånedersregelen.
Applikasjonsansvarlig bekrefter samsvar med testet kodeversjon; DBA skal ikke
måtte lese Kotlin-kode. Sett godkjent `CURRENT SCHEMA` og bruk miljøets
EXPLAIN-tabeller. Eksempel-ID-er og grense erstattes med representative verdier.

Dette er **EXPLAIN-kall, ikke en slettesekvens**. Kjør ikke DML-setningene
separat i produksjon. Kandidatoppslaget er identisk i begge faser og vises én
gang. To ID-er illustrerer batchen; gjenta med maksimal godkjent batch
(standard 100 ID-er). `FETCH FIRST 201` tilsvarer standard referansegrense 200 + 1.

```sql
-- Snapshot ved rundestart
EXPLAIN PLAN SET QUERYNO = 731701 FOR
SELECT CURRENT TIMESTAMP AS RUNDESTART,
       MIN(TRANS_TILSTAND_ID) AS MIN_ID,
       MAX(TRANS_TILSTAND_ID) AS MAX_ID
FROM T_TRANS_TILSTAND;

-- Kandidater i et avgrenset ID-intervall (begge faser)
EXPLAIN PLAN SET QUERYNO = 731702 FOR
SELECT TRANS_TILSTAND_ID
FROM T_TRANS_TILSTAND
WHERE TRANS_TILSTAND_ID BETWEEN 20000000 AND 20000999
  AND DATO_ENDRET < TIMESTAMP('2026-04-12 08:00:00.123456')
ORDER BY TRANS_TILSTAND_ID;

-- Fase 1: slett bare urefererte, gamle tilstander
EXPLAIN PLAN SET QUERYNO = 731703 FOR
DELETE FROM T_TRANS_TILSTAND s
WHERE s.TRANS_TILSTAND_ID IN (20000000, 20000001)
  AND s.DATO_ENDRET < TIMESTAMP('2026-04-12 08:00:00.123456')
  AND NOT EXISTS (
      SELECT 1 FROM T_TRANSAKSJON t
      WHERE t.TRANS_TILSTAND_ID = s.TRANS_TILSTAND_ID
  );

-- Fase 2: bekreft alder og behold oppdateringslåser til commit
EXPLAIN PLAN SET QUERYNO = 731704 FOR
SELECT TRANS_TILSTAND_ID
FROM T_TRANS_TILSTAND
WHERE TRANS_TILSTAND_ID IN (20000000, 20000001)
  AND DATO_ENDRET < TIMESTAMP('2026-04-12 08:00:00.123456')
FOR UPDATE WITH RS USE AND KEEP UPDATE LOCKS;

-- Fase 2: avgrenset referanseprobe for bekreftede ID-er
EXPLAIN PLAN SET QUERYNO = 731705 FOR
SELECT TRANSAKSJON_ID
FROM T_TRANSAKSJON
WHERE TRANS_TILSTAND_ID IN (20000000, 20000001)
FETCH FIRST 201 ROWS ONLY;

-- Fase 2: nullstill bare referanser til bekreftede ID-er
EXPLAIN PLAN SET QUERYNO = 731706 FOR
UPDATE T_TRANSAKSJON
SET TRANS_TILSTAND_ID = NULL
WHERE TRANS_TILSTAND_ID IN (20000000, 20000001);

-- Fase 2: slett de samme bekreftede, gamle tilstandene
EXPLAIN PLAN SET QUERYNO = 731707 FOR
DELETE FROM T_TRANS_TILSTAND
WHERE TRANS_TILSTAND_ID IN (20000000, 20000001)
  AND DATO_ENDRET < TIMESTAMP('2026-04-12 08:00:00.123456');
```

Ved funksjonstesting kjøres fase 2-oppslagene og endringene i samme transaksjon.
Bare ID-er som alderssjekken bekreftet og låste, brukes videre.
Referanseoverskridelse eller avvik mellom bekreftet og slettet antall gir
rollback, ikke commit.

Bruk representativ grense, sparsomme/tette intervaller, maksimal godkjent batch
og referansegrense + 1. Arkiver PLAN_TABLE-uttrekk med indeksvalg, matching,
join og sortering. Mål også driverens prepared statements med bundne parametere;
EXPLAIN alene viser ikke faktisk kostnad eller venting.

**GO:** DBA godkjenner avgrensede tilgangsstier og målt kostnad, også for snapshot og FK-kontroll.
**NO-GO:** uavgrenset tablespace-/parent-skanning eller uakseptabel sortering/kostnad; pauser gjør ikke én tung SQL-enhet trygg.

## Resultat og beslutning

| Test | Utfører/dato | Resultat og evidens | Avvik/tiltak/retest | GO/NO-GO |
| --- | --- | --- | --- | --- |
| T01 EXPLAIN | Ikke registrert | Ikke testet | Ikke avklart | NO-GO |

**DBA-GO for produksjonsprøve:** T01 og T02 bestått, ingen blokkerende avvik,
og datert sikkerhetsgodkjenning fra DBA/applikasjonsansvarlig arkivert.
Produksjonsprotokollens startvilkår må også være oppfylt.
**GO for fast drift:** krever i tillegg godkjent produksjonsprøve (T03).
**Ellers NO-GO:** registrer årsak, tiltak og retest.
Endret SQL, indeks, isolasjon, timeout eller økte batch-/referansegrenser
krever ny vurdering av berørte tester. Ingen automatisk aktivering.

Se [driftsrutinen](../drift/tilstandsrydding.md) for konfigurasjon og overvåking.
