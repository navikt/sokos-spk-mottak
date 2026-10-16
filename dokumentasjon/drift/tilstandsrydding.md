# Rydding av gamle transaksjonstilstander

## Formål og aktivering

`TilstandsRyddeService` fjerner gamle rader fra Db2 `T_TRANS_TILSTAND`.
Serveren deles med kritiske tjenester, så arbeidet avgrenses, strupes og
kan gjenopptas. Komponenten er ikke registrert som jobb og starter ikke
automatisk. Cron-integrasjon og funksjonsbryter er neste PRs ansvar.

**Ikke aktiver sletting før
[DBA-testspesifikasjonen](../tester/tilstandsrydding-db2.md) gir samlet GO.**
Testprotokollen krever konkrete sikkerhetsgrenser, EXPLAIN,
samtidighets-/låsetester og dokumentert godkjenning før en begrenset prøve.
Akseptabel last på delt maskinvare avgjøres deretter med
[produksjonsprotokollen](tilstandsrydding-produksjonsprove.md), med overvåking
og mulighet for å stoppe. Startverdiene under er ikke bevis på akseptabel last.

## Oppbevaring og avstemming

Tilstander med `DATO_ENDRET` strengt før Db2-tidspunktet ved rundestart minus
seks kalendermåneder kan slettes. Likhet med grensen beholdes.
Grensen fryses for hele runden, også ved gjenopptakelse. Oppbevaring kan økes,
men ikke settes under seks måneder uten ny forretningsavklaring.

**Avstemming skal kjøres innen tre måneder.** Dette godkjente kravet gir
margin mot seks måneders historikk. Feilkoder og feilmeldinger i slettede
tilstander er ikke lenger tilgjengelige for historisk avstemming eller
etterkjøring. Ikke regn med at gamle feiltransaksjoner fortsatt vises i
avstemmingsdetaljene etter at deres siste tilstand er fjernet.

Transaksjoner og returdata slettes ikke. Ved sletting av en referert tilstand
nullstilles bare `T_TRANSAKSJON.TRANS_TILSTAND_ID`; beløp, status, metadata og
andre forretningsfelter beholdes.

## Gjennomføring

Bare én arbeider får kjøre på tvers av instanser. En dedikert PostgreSQL-sesjon
holder advisory lock `0x53504B5259444445`; opptatt lås betyr at kallet avsluttes
uten Db2-arbeid. Tap av låseforbindelsen stopper nye Db2-enheter.

En ny runde lagrer fast Db2-grense og minste/største tilstands-ID i
Postgres `tilstandsrydding_status`. Arbeideren undersøker små ID-intervaller
via primærnøkkelindeksen, ikke et uavgrenset aldersoppslag.

Hvert intervall har to faser:

1. Slett gamle, urefererte tilstander i små transaksjoner uten parent-UPDATE.
2. Les samme intervall på nytt, lås og bekreft gjenværende gamle tilstander,
   nullstill deres eksisterende transaksjonsreferanser og slett atomisk.

Intervallmarkøren lagres først når begge faser er ferdige. Et krasj etter
Db2-commit før Postgres-oppdatering gjentar intervallet; allerede slettede
rader gir null nye slettinger. Etter fullført runde begynner neste kall
fra begynnelsen med en ny grense. Nye gamle rader og sene lave ID-er vurderes da.

Regelversjon eller oppbevaring endret underveis gir ny runde, ikke videreføring
av gammel markør med ny regel. Flytt aldri markøren manuelt fremover for å
omgå et problem; det kan hoppe over uslettede rader.

## Konfigurasjon

Disse innstillingene aktiverer ikke jobben. Angi dem bare som del av godkjent
innfasing. Parserfeil og ugyldige verdier skal stoppe konfigurasjonsbyggingen.

| Miljøvariabel | Standard | Betydning |
| --- | --- | --- |
| `TILSTANDS_RYDDING_OPPBEVARING_MAANEDER` | 6 | Kalendermåneder, minst 6 |
| `TILSTANDS_RYDDING_INTERVALL_BREDDE` | 1000 | ID-er per kandidatintervall, 1–10000 |
| `TILSTANDS_RYDDING_BATCH_STORRELSE` | 100 | Tilstander per slettebatch, 1–100 |
| `TILSTANDS_RYDDING_MAKS_REFERANSER_PER_BATCH` | 200 | Parent-referanser per batch; overskridelse gir rollback og stopp |
| `TILSTANDS_RYDDING_PAUSE_MIN_SEKUNDER` | 1 | Minste pause mellom Db2-enheter |
| `TILSTANDS_RYDDING_PAUSE_MAKS_SEKUNDER` | 30 | Største adaptive pause |
| `TILSTANDS_RYDDING_TERSKEL_ENHET_SEKUNDER` | 1 | Langsommere enhet dobler pausen; raskere halverer mot minimum |
| `TILSTANDS_RYDDING_MAKS_KJOERETID_SEKUNDER` | 900 | Kjøretidsbudsjett per kall |
| `TILSTANDS_RYDDING_MAKS_FORSOEK` | 3 | Totalt antall forsøk per låsefeilet enhet, inkludert første |
| `TILSTANDS_RYDDING_SPORRING_TIMEOUT_SEKUNDER` | 5 | Query-timeout; reduseres ved lite gjenværende kjøretid |

Alle Db2-enheter strupes, også kandidatlesing, tomme intervaller og
faseovergang. Pausene holdes uten Db2-forbindelse eller transaksjonslåser.
Låsefeil gir rollback og maksimumspause før eventuelt nytt forsøk.
Ukjent SQL-feil, fremmednøkkelfeil, referanseoverskridelse eller Postgres-feil
stopper kjøringen synlig. Ikke deaktiver constraints eller hopp over feil-ID-er.

Kjøretidsbudsjettet er en grense for å starte nytt arbeid, ikke en garanti om
at hele servicekallet returnerer innen nøyaktig 900 sekunder. Statement-timeout
dekker ikke venting på poolforbindelse, commit, rollback eller avsluttende
opplåsing. Disse operasjonene må få godkjente, målte grenser gjennom pool,
driver/nettverk og Db2-konfigurasjon i DBA-testen. Ved utløpt budsjett starter
ingen ny dataenhet, men nødvendig rollback og opplåsing skal fortsatt utføres.

## Observerbarhet

Statistikken ligger i eksisterende Prometheus-registry og metrics-endepunkt.
Logs inneholder aggregerte antall og stoppårsak, ikke fødselsnummer,
feilhistorikk eller ID-lister.

| Serie (prefiks `sokos_spk_mottak_`) | Bruk |
| --- | --- |
| `tilstandsrydding_kandidater_total{fase}` | Kandidater fra oppslag; en rad kan telles flere ganger |
| `tilstandsrydding_slettede_total{fase}` | Faktisk sletting etter commit |
| `tilstandsrydding_nullstilte_referanser_total` | Faktisk nullstilling etter commit |
| `tilstandsrydding_sqlfeil_total{type}` | Feil per forsøk; typer las, timeout, fremmednokkel, annen |
| `tilstandsrydding_kjoringer_total{utfall}` | Utfall fullfort, budsjett, avbrutt, opptatt, feil |
| `tilstandsrydding_runder_fullfort_total` | Fullførte runder lagret i Postgres |
| `tilstandsrydding_pause_sekunder` | Gjeldende pause |
| `tilstandsrydding_markor_id` / `tilstandsrydding_ovre_id` | Lagret intervallmarkør og rundens øvre ID |
| `tilstandsrydding_aktiv` | Instansen eier kjøringslåsen |
| `tilstandsrydding_siste_fullfort_timestamp_sekunder` | Tidspunkt for sist fullførte runde |
| `database_call{className,method}` / `service_call{className,method}` | Eksisterende repository-/servicetimere |

Målinger er per instans. Kandidater er ikke antall unike tilstander, og en
markør kan starte på nytt ved ny runde. Slettetellere er ikke et transaksjonelt
revisjonsregnskap: et krasj etter commit før metric-oppdatering kan undertelle.

Følg særlig nye SQL-feil, stigende pause, stillestående fremdrift gjennom flere
kjøringer og lenge siden siste fullførte runde. Følg også Db2 CPU/I/O/logg/låser
og responstiden til andre tjenester; applikasjonens egne timere dekker ikke
maskinvarens totale belastning.

## Innfasing, stopp og feil

1. Kontroller at målrettet valideringsreparasjon er på plass: nye transaksjoner
   får referanser, historiske NULL-referanser besøkes ikke på nytt.
2. Gjennomfør DBA-protokollens sikkerhetstester og dokumenter GO for en
   begrenset produksjonsprøve. Eventuell endring av indekser gjøres av DBA.
3. Innfør cron/funksjonsbryter i egen PR med de samme stopp-/låsekravene.
4. Gjennomfør den overvåkede produksjonsprøven med avtalte stoppgrenser.
   Fast drift tillates først etter godkjent prøve; økning krever ny vurdering.

Kallet skal få en stoppindikator som den senere jobb-integrasjonen kobler til
nedstenging/funksjonsbryter. Avbrutt pause stopper raskt. En påbegynt SQL-enhet
må nå avklart commit/rollback innen den DBA-verifiserte avbruddsgrensen.
Det finnes ennå ingen cron eller operativ funksjonsbryter å slå av.

Ved gjentatte feil: stopp videre kjøring, behold status, undersøk SQLSTATE/
SQLCODE og årsak med DBA. Bekreft rollback før ny kjøring. Gjenopptakelse
skal bruke samme låste serviceinngang, aldri løse SQL-slettinger som omgår
markør, alderskontroll eller referansegrense.

Etter store runder vurderer DBA `RUNSTATS` og eventuell `REORG`.
DELETE alene garanterer ikke frigjort fysisk diskplass. Komponenten utfører
ikke slike vedlikeholdsoperasjoner automatisk.
