# Produksjonsprøve: belastning fra tilstandsrydding

**Status: ikke gjennomført. Ingen tillatelse til fast drift.**

Formålet er å måle faktisk påvirkning på delt Db2-maskinvare og stoppe ryddingen
dersom påvirkningen er uakseptabel. Dette er en overvåket, avgrenset prøve med
ekte sletting, ikke en stresstest. Feilinjeksjon, krasjtester og sammenligning
av alternative slettealgoritmer skal ikke utføres i produksjon.

## Før start

DBA og applikasjonsansvarlig godkjenner sikkerhetstestene i
[DBA-protokollen](../tester/tilstandsrydding-db2.md). De avtaler med berørte
tjenesteeiere prøvetidspunkt, målevindu, observatører og følgende verdier:

| Avtale | Registrert verdi |
| --- | --- |
| Kodeversjon, Db2-miljø og konfigurasjon | Ikke registrert |
| Prøvens varighet, intervallbredde, batch-/referansegrense og pauser | Ikke avtalt |
| Operatør med stoppmyndighet og tilgjengelig DBA | Ikke registrert |
| Baselineperiode, målekilder og observasjonsfrekvens | Ikke avtalt |
| CPU, getpages/I/O og loggskriving: absolutte grenser og tillatt økning | Ikke avtalt |
| Låseventing, eskalering og deadlock: stoppgrenser | Ikke avtalt |
| Andre tjenesters responstid/feilrate: grenser per tjeneste | Ikke avtalt |
| Maksimal tid fra stoppbeskjed til avsluttet arbeid/frigitte låser | Ikke avtalt |
| Konkret stoppkommando og bekreftelse på ingen automatisk omstart | Ikke registrert |

Manglende verdi eller målesignal betyr **NO-GO for start**. Bruk små,
DBA-godkjente batcher og intervaller, lange pauser og kort kjøretid.
Ikke øk belastningen automatisk.

Den senere jobb-/funksjonsbryterintegrasjonen må ha en prøvd stoppmekanisme
som både hindrer nye kjøringer og stopper nye enheter i den pågående kjøringen.
Den må kunne avbryte venting eller ha en dokumentert, akseptabel reaksjonstid.
En bryter som bare stopper neste cron-kall er utilstrekkelig.
Denne integrasjonen finnes ikke ennå; protokollen gir ikke alene mulighet til
å starte eller stoppe jobben.

Stopp er ikke øyeblikkelig: pågående SQL må nå bekreftet commit/rollback.
Statement-timeout dekker ikke nødvendigvis poolventing, commit eller rollback.
Disse grensene må være avklart i sikkerhetstestene. DBA avtaler også en
beredskapsprosedyre for å avbryte rydderens identifiserte sesjon dersom normal
stopp overskrider grensen; eventuell rollback-belastning må tas med i vurderingen.

## Gjennomføring og stopp

1. Mål baseline med ordinær trafikk uten rydding. Kontroller målesignaler og
   at ingen annen vedlikeholdsaktivitet gjør sammenligningen misvisende.
2. Start én avgrenset kjøring under tilsyn. Følg både Prometheus-statistikken
   og Db2-/tjenestemålingene; rydderens egne timere er ikke et lastmål for andre.
3. **Stopp straks** ved grenseoverskridelse, låseeskalering, uventet påvirkning,
   bortfall av overvåking eller beskjed fra DBA/berørt tjenesteeier.
4. Hindre nye kjøringer og gi stoppbeskjed til den aktive arbeideren.
   Bekreft at ingen ny Db2-enhet starter, avklar siste transaksjons utfall,
   og kontroller at Db2-låser og PostgreSQL-kjøringslås er frigitt.
   Bruk avtalt DBA-beredskap hvis stopptiden overskrides.
5. Fortsett måling etter stopp til annen trafikk er tilbake på akseptabelt nivå.
   Registrer tidslinje, maksimal påvirkning, faktisk stopptid og lagret markør.

Stopp reverserer ikke allerede committede slettinger. Behold fremdriftsstatus;
ikke flytt markøren manuelt. Omkjøring må bruke samme låste serviceinngang.

## Beslutning

**GO:** alle avtalte grenser holdes gjennom hele prøven og etterperioden,
normal stopp er demonstrert i produksjon, og DBA/applikasjonsansvarlig godkjenner
den testede konfigurasjonen med berørte tjenesteeiere.

**NO-GO:** overskridelse, ukjent påvirkning eller utilstrekkelig stopp.
La planlagt kjøring være deaktivert. Registrer årsak, tiltak og eventuell ny
prøve med lavere batch/intervall, lengre pauser eller rettet stoppmekanisme.

Arkiver målinger og datert beslutning. Et kort forsøk godkjenner ikke all
fremtidig last: fast drift beholder stoppgrenser og overvåking.
Økt belastning eller endret SQL/indeks/isolasjon krever ny vurdering og ved
behov ny avgrenset prøve.
