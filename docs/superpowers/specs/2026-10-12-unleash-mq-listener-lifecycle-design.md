# Unleash-styrte MQ-listenere

## Mål

La Unleash-flagget `sokos-spk-mottak.listenerEnabled` slå alle tre MQ-listenerne av og på uten omstart av appen. En recurring job skal kontrollere flagget hvert minutt.

## Beslutninger

- `MQ_LISTENER_ENABLED` og `AVREGNING_LISTENER_ENABLED` gjelder fortsatt ved oppstart. De bestemmer hvilke lyttere podden får lov til å starte. Unleash kan slå av og på lyttere innenfor denne grensen.
- Hver pod må utføre kontrollen selv, fordi hver pod eier sine egne JMS-consumere. Registrer én db-scheduler-task-instans per pod med en stabil pod-identitet. Ikke bruk én delt task-instans, som bare ville kjørt på én pod.
- Ved endret flaggtilstand lukker koordinatoren de aktive listenerne når flagget er av, og oppretter og starter de tillatte listenerne når flagget er på.
- Listenerne må kunne lukkes fullstendig, inkludert JMS-consumer og JMSContext. Koordinatoren beholder oversikt over aktiv tilstand for å unngå å opprette duplikate consumers.
- Hvis flaggtilstanden ikke kan leses, beholder podden siste kjente tilstand. Feilen logges, og neste kjøring prøver igjen.
- En listener som allerede har hentet en melding, fullfører gjeldende behandling før den lukkes. Bekreftelse og feilhåndtering følger eksisterende MQ-mønster.

## Oppstart og flaggkontroll

Opprett en listener-koordinator per pod. Ved oppstart starter den listenerne som miljøvariablene tillater, hvis `sokos-spk-mottak.listenerEnabled` er på. Registrer samtidig en recurring task med task-id basert på pod-identiteten og ett minutts intervall. Tasken leser flagget og ber koordinatoren avstemme ønsket tilstand med aktive consumers.

Unleash-klienten henter flaggverdier separat fra recurring job. Endringer blir derfor synlige etter både Unleash-klientens oppdateringsintervall og neste jobbkjøring. Løsningen lover ikke øyeblikkelig stans.

## Feil og livssyklus

En feil når Unleash-tilstanden leses skal ikke endre listenerne. En feil når consumers startes eller lukkes skal logges og forplantes til db-scheduler, slik at feilen blir synlig og neste kjøring kan forsøke avstemming på nytt. Koordinatorens interne tilstand må bare oppdateres etter vellykket start eller lukking.

Ved pod-stans lukkes alle aktive listenerressurser. Flere podder kan kjøre samme periodiske task-type, men hver pod-instans styrer bare sine lokale MQ-listenere.

## Tester

- Flagget av lukker alle tillatte, aktive listenerne én gang.
- Flagget på starter alle listenerne som miljøvariablene tillater, én gang.
- Gjentatte kjøringer med uendret flagg oppretter ikke duplikate consumers.
- Miljøvariabler som er av hindrer den aktuelle listeneren i å starte selv om flagget er på.
- Feil ved lesing av flagget beholder siste aktive tilstand.
- Feil ved start eller lukking rapporteres til scheduler, og en senere kjøring kan avstemme tilstanden.
- Lukking av koordinatoren lukker alle aktive JMS-ressurser.
