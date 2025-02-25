# sokos-spk-mottak

# Innholdsoversikt

* [1. Dokumentasjon](dokumentasjon/dokumentasjon.md)
* [2. Funksjonelle krav](#2-funksjonelle-krav)
* [3. Utviklingsmiljø](#3-utviklingsmiljø)
* [4. Programvarearkitektur](#4-programvarearkitektur)
* [5. Deployment](#5-deployment)
* [6. Autentisering](#6-Autentisering)
* [7. Drift og støtte](#7-drift-og-støtte)
* [8. Henvendelser](#8-henvendelser)

---

# 2. Funksjonelle Krav

sokos-spk-mottak er en applikasjon som mottar utbetalings- og trekktransaksjoner fra Statens Pensjonskasse (SPK) og sender utbetalings- og trekkoppdrag til Oppdrag Z

# 3. Utviklingsmiljø

### Forutsetninger

* Java 21
* Gradle
* [Kotest](https://plugins.jetbrains.com/plugin/14080-kotest) plugin for å kjøre tester
* [Docker](https://www.docker.com/) for å kjøre testcontainers

### Bygge prosjekt

Du må ha Docker på for å bygge prosjektet da bygging også kjører tester med testcontainers.

```
./gradlew clean build shadowJar
```

### Lokal utvikling

For å kjøre applikasjonen lokalt må du gjøre følgende:

- Kjør scriptet [setupLocalEnvironment.sh](setupLocalEnvironment.sh)
  ```
  chmod 755 setupLocalEnvironment.sh && ./setupLocalEnvironment.sh
  ```
  Denne vil opprette [default.properties](defaults.properties) med alle environment variabler du trenger for å kjøre
  applikasjonen som er definert i [PropertiesConfig](src/main/kotlin/no/nav/sokos/spk/mottak/config/PropertiesConfig.kt).

- Må også ha med `db2jcc_license_cisuz.jar` for at `sokos-spk-mottak` skal kunne koble seg til DB2.
  Denne må ligge i `Classpath` som JVM options. F.eks på Mac/Linux blir følgende lagt til:
```
-cp $Classpath$:path/db2jcc_license_cisuz.jar
```

### Miljøer

`sokos-spk-mottak` kjøres i følgende miljøer:
- q1
- qx
- prod

# 4. Programvarearkitektur

TODO

# 5. Deployment

Distribusjon av tjenesten er gjort med bruk av Github Actions.
[sokos-spk-mottak CI / CD](https://github.com/navikt/sokos-spk-mottak/actions)

Push/merge til main branch direkte er ikke mulig. Det må opprettes PR og godkjennes før merge til main branch.
Når PR er merged til main branch vil Github Actions bygge og deploye til dev-fss og prod-fss.
Har også mulighet for å deploye manuelt til testmiljø ved å deploye PR.

# 6. Autentisering

Applikasjonen bruker [AzureAD](https://docs.nais.io/security/auth/azure-ad/) for å sikre at kun autoriserte brukere har tilgang til tjenesten.

# 7. Drift og støtte

Du kan trigge jobber fra Spk Mottak Dashboard i Utbetalingsportalen. For å gjøre dette kreves det at du har tilgang til riktig AD-gruppe for skjermbildet.
- [Spk Mottak Dashboard i test](https://utbetalingsportalen.intern.dev.nav.no/spk-mottak)
- [Spk Mottak Dashboard i prod](https://utbetalingsportalen.intern.nav.no/spk-mottak)

Applikasjonen bruker sikker SFTP for filoverføring til/fra SPK. Problemer med SFTP kan kan henvendes til [#tech-linux](https://nav-it.slack.com/archives/CA2CM7QTX)

### Logging

https://logs.adeo.no.

Feilmeldinger og infomeldinger som ikke innheholder sensitive data logges til data view `Applikasjonslogger`.  
Sensetive meldinger logges til data view `Securelogs` [sikker-utvikling/logging](https://sikkerhet.nav.no/docs/sikker-utvikling/logging)).

- Filter for Produksjon
    * application:sokos-spk-mottak AND envclass:p

- Filter for Dev
    * application:sokos-spk-mottak AND envclass:q

### Kubectl

For dev-fss:

```shell script
kubectl config use-context dev-fss
kubectl get pods -n okonomi | grep sokos-spk-mottak
kubectl logs -f sokos-spk-mottak-<POD-ID> --namespace okonomi -c sokos-spk-mottak
```

For prod-fss:

```shell script
kubectl config use-context prod-fss
kubectl get pods -n okonomi | grep sokos-spk-mottak
kubectl logs -f sokos-spk-mottak-<POD-ID> --namespace okonomi -c sokos-spk-mottak
```

### Alarmer

Vi bruker [nais-alerts](https://doc.nais.io/observability/alerts) for å sette opp alarmer.
Disse finner man konfigurert i [.nais/alerts-dev.yaml](.nais/alerts-dev.yaml) filen og [.nais/alerts-prod.yaml](.nais/alerts-prod.yaml).
Alarmene blir publisert i Slack kanalen [#team-mob-alerts-dev](https://nav-it.slack.com/archives/C042SF2FEQM) og [#team-mob-alerts-prod](https://nav-it.slack.com/archives/C042ESY71GX).

### Grafana

- [sokos-spk-mottak](https://grafana.nav.cloud.nais.io/d/fdrtp6qv623ggf/sokos-spk-mottak?orgId=1&refresh=30s)

---

# 8. Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på Github.
Interne henvendelser kan sendes via Slack i kanalen [#po-utbetaling](https://nav-it.slack.com/archives/CKZADNFBP)

