# sokos-spk-mottak

# Innholdsoversikt

* [1. Funksjonelle krav](#1-funksjonelle-krav)
* [2. Utviklingsmiljø](#2-utviklingsmiljø)
* [3. Programvarearkitektur](#3-programvarearkitektur)
* [4. Deployment](#4-deployment)
* [5. Autentisering](#5-autentisering)
* [6. Drift og støtte](#6-drift-og-støtte)
* [7. Henvendelser](#7-henvendelser)

---

# 1. Funksjonelle Krav

sokos-spk-mottak er en applikasjon som mottar utbetalings- og trekktransaksjoner fra Statens Pensjonskasse (SPK) og sender utbetalings- og trekkoppdrag til Oppdrag Z

# 2. Utviklingsmiljø

### Forutsetninger

* Java 21
* Gradle 8.7
* [Kotest](https://plugins.jetbrains.com/plugin/14080-kotest) plugin for å kjøre tester
* [Docker](https://www.docker.com/) for å kjøre testcontainers

### Bygge prosjekt

Du må ha Docker på for å bygge prosjektet da bygging også kjører tester med testcontainers.
Du må være i roten av prosjektet for å kjøre bygging.

```
./gradlew build shadowJar
```

### Lokal utvikling

For å kjøre applikasjonen lokalt må du gjøre følgende:

- Kjør scriptet [setupLocalEnvironment.sh](setupLocalEnvironment.sh)
  ```
  chmod 755 setupLocalEnvironment.sh && ./setupLocalEnvironment.sh
  ```
  Denne vil opprette [default.properties](defaults.properties) med alle environment variabler du trenger for å kjøre
  applikasjonen som er definert i [PropertiesConfig](src/main/kotlin/no/nav/sokos/spk.mottak/config/PropertiesConfig.kt).

# 3. Programvarearkitektur

Legg ved skissediagram for hvordan arkitekturen er bygget

# 4. Deployment

Distribusjon av tjenesten er gjort med bruk av Github Actions.
[sokos-spk-mottak CI / CD](https://github.com/navikt/sokos-spk-mottak/actions)

Push/merge til main branche vil teste, bygge og deploye til testmiljø og produksjonsmiljø.
Har også mulighet for å deploye manuelt til testmiljø ved å lage egen branch.

# 7. Autentisering

Applikasjonen bruker [AzureAD](https://docs.nais.io/security/auth/azure-ad/) for å sikre at kun autoriserte brukere har tilgang til tjenesten.

# 6. Drift og støtte

### Logging

Vi logger til https://logs.adeo.no.

Feilmeldinger og infomeldinger som ikke innheholder sensitive data logges til indeksen `logstash-apps`, mens meldinger
som inneholder sensitive data logges til indeksen `tjenestekall`.

- Filter for Produksjon
    * application:sokos-spk-mottak AND envclass:p

- Filter for Dev
    * application:sokos-spk-mottak AND envclass:q

[sikker-utvikling/logging](https://sikkerhet.nav.no/docs/sikker-utvikling/logging) - Anbefales å lese

- Filter for sikkerhet logs på https://logs.adeo.no
    * Bytte Change index pattern fra: logstash-* til: tjenestekall-*
    * Bruk samme filter for dev og prod som er vist over

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
Disse finner man konfigurert i [.nais/alerts-dev.yaml](.nais/alerts-dev.yaml) filen og [.nais/alerts-prod.yaml](.nais/alerts-prod.yaml)

### Grafana

- [appavn](url)

---

# 7. Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på Github.
Interne henvendelser kan sendes via Slack i kanalen #po-utbetaling

