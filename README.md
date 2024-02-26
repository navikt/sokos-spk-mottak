# sokos-spk-mottak

# Innholdsoversikt
* [1. Funksjonelle krav](#1-funksjonelle-krav)
* [2. Utviklingsmiljø](#2-utviklingsmiljø)
* [3. Programvarearkitektur](#3-programvarearkitektur)
* [4. Deployment](#4-deployment)
* [5. Autentisering](#5-autentisering)
* [6. Drift og støtte](#6-drift-og-støtte)
* [7. Swagger](#7-swagger)
* [8. Henvendelser](#8-henvendelser)
---

# 1. Funksjonelle Krav
Hva er oppgaven til denne applikasjonen

# 2. Utviklingsmiljø
### Forutsetninger
* Java 21
* Gradle 8.4
* [Kotest](https://plugins.jetbrains.com/plugin/14080-kotest) plugin for å kjøre tester

### Bygge prosjekt
```
./gradlew build shadowJar
```

### Lokal utvikling
For å kjøre applikasjonen lokalt må du gjøre følgende:
-  Kjør scriptet [setupLocalEnvironment.sh](setupLocalEnvironment.sh)
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

Push/merge til main branche vil teste, bygge og deploye til produksjonsmiljø og testmiljø.
Har også mulighet for å deploye til testmiljø ved å lage egen branch uten push til main branch.

# 7. Autentisering
Applikasjonen bruker [AzureAD](https://docs.nais.io/security/auth/azure-ad/) autentisering

# 6. Drift og støtte

### Logging
Hvor finner jeg logger? Hvordan filtrerer jeg mellom dev og prod logger?

[sikker-utvikling/logging](https://sikkerhet.nav.no/docs/sikker-utvikling/logging) - Anbefales å lese

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

# 7. Swagger
Hva er url til Lokal, dev og prod?

# 8. Henvendelser og tilgang
   Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på Github.
   Interne henvendelser kan sendes via Slack i kanalen #po-utbetaling

