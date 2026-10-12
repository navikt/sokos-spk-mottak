# Unleash-styrte MQ-listenere

## Mål

La Unleash-flagget `sokos-spk-mottak.listenerEnabled` slå alle tre MQ-listenerne av og på uten omstart av appen. Hver pod skal kontrollere flagget lokalt hvert minutt.

## Beslutninger

- `MQ_LISTENER_ENABLED` og `AVREGNING_LISTENER_ENABLED` gjelder fortsatt ved oppstart. De bestemmer hvilke lyttere podden får lov til å starte. Unleash kan slå av og på lyttere innenfor denne grensen.
- Hver pod må utføre kontrollen selv, fordi hver pod eier sine egne JMS-consumere. Bruk en lokal `ScheduledExecutorService` per pod. db-scheduler fordeler kjøringer mellom podder og garanterer ikke at en pod kjører sin egen task-instans.
- Ved endret flaggtilstand lukker koordinatoren de aktive listenerne når flagget er av, og oppretter og starter de tillatte listenerne når flagget er på.
- Listenerne må kunne lukkes fullstendig, inkludert JMS-consumer og JMSContext. Koordinatoren beholder oversikt over aktiv tilstand for å unngå å opprette duplikate consumers.
- Hvis flaggtilstanden ikke kan leses, beholder podden siste kjente tilstand. Feilen logges, og neste kjøring prøver igjen. Før podden har lest flagget første gang, starter den ingen listener før en vellykket lesing viser at flagget er på.
- Når en consumer lukkes, kan en melding som allerede er levert, men ikke bekreftet, bli levert på nytt av MQ. Ikke bekreft meldingen bare fordi listeneren stenges. Eksisterende bekreftelse og feilhåndtering gjelder fortsatt.

## Oppstart og flaggkontroll

Opprett en listener-koordinator per pod og en lokal `ScheduledExecutorService`. Kjør første flaggsjekk ved oppstart, og kjør deretter kontrollen med ett minutts forsinkelse etter hver fullførte sjekk. Jobben leser `sokos-spk-mottak.listenerEnabled` og ber koordinatoren avstemme ønsket tilstand med aktive consumers. Ved første vellykkede flagglesing starter koordinatoren listenerne miljøvariablene tillater, hvis flagget er på.

Unleash-klienten henter flaggverdier uavhengig av den periodiske jobben. Endringer blir derfor synlige etter både Unleash-klientens oppdateringsintervall og neste kontroll. Løsningen lover ikke øyeblikkelig stans.

## Feil og livssyklus

En feil når Unleash-tilstanden leses skal ikke endre listenerne. En feil når consumers startes eller lukkes skal logges, og må ikke stoppe den periodiske jobben. Jobben prøver avstemming på nytt ved neste kjøring. Koordinatorens interne tilstand må bare oppdateres etter vellykket start eller lukking.

Ved pod-stans stoppes executor-en, og alle aktive listenerressurser lukkes. Hver pod styrer bare sine lokale MQ-listenere.

## Tester

- Flagget av lukker alle tillatte, aktive listenerne én gang.
- Flagget på starter alle listenerne som miljøvariablene tillater, én gang.
- Gjentatte kjøringer med uendret flagg oppretter ikke duplikate consumers.
- Miljøvariabler som er av hindrer den aktuelle listeneren i å starte selv om flagget er på.
- Feil ved lesing av flagget beholder siste aktive tilstand.
- Feil ved start eller lukking logges, og en senere kjøring kan avstemme tilstanden.
- Lukking av koordinatoren lukker alle aktive JMS-ressurser.
- En melding som ikke er bekreftet når consumeren lukkes, blir ikke bekreftet av koordinatoren.
