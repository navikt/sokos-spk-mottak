# sokos-spk-mottak

Kotlin/Ktor-applikasjon som mottar utbetalings- og trekktransaksjoner fra SPK og sender oppdrag til Oppdrag Z.

* [1. Arkitektur](#1-arkitektur)
* [2. Utviklingsmiljø](#2-utviklingsmiljø)
* [3. Miljøer](#3-miljøer)
* [4. Autentisering og autorisasjon](#4-autentisering-og-autorisasjon)
* [5. Deployment](#5-deployment)
* [6. Drift og støtte](#6-drift-og-støtte)
* [7. Henvendelser](#7-henvendelser)

---

## 1. Arkitektur

Applikasjonen kjører jobber med [db-scheduler](https://github.com/kagkarlsson/db-scheduler), enten etter
cron-uttrykk eller når de trigges fra [admin-dashboardet](../frontend/README.md).

Applikasjonen integrerer med:

- **SFTP** for å lese filer fra og skrive returfiler til SPK
- **DB2** for transaksjoner, filinformasjon og leveattester
- **PostgreSQL** for jobbplanleggingen i db-scheduler
- **IBM MQ** for å sende utbetalinger, trekk og avstemming til Oppdrag Z, og ta imot kvitteringer og avregningsgrunnlag
- **PDL** for å hente personidenter

Detaljert løsningsbeskrivelse, domenemodell og testdokumentasjon ligger i [dokumentasjon](dokumentasjon/dokumentasjon.md).

## 2. Utviklingsmiljø

### Forutsetninger

* Java 25
* [Gradle](https://gradle.org/)
* [Kotest](https://plugins.jetbrains.com/plugin/14080-kotest)-plugin for å kjøre tester
* [Docker](https://www.docker.com/) for å kjøre Testcontainers

### Bygge

Bygget kjører også testene med Testcontainers, så Docker må kjøre.

```shell
./gradlew build installDist
```

### Kjøre lokalt

1. Kjør [setupLocalEnvironment.sh](setupLocalEnvironment.sh):

   ```shell
   chmod 755 setupLocalEnvironment.sh && ./setupLocalEnvironment.sh
   ```

   Skriptet lager `defaults.properties` med miljøvariablene som
   [PropertiesConfig](src/main/kotlin/no/nav/sokos/spk/mottak/config/PropertiesConfig.kt) trenger.

2. Legg `db2jcc_license_cisuz.jar` på classpath, slik at applikasjonen kan koble seg til DB2. På Mac og Linux legger du
   dette til i JVM-options:

   ```
   -cp $Classpath$:path/db2jcc_license_cisuz.jar
   ```

## 3. Miljøer

| Miljø | Cluster | URL |
|---|---|---|
| q1 | dev-fss | https://sokos-spk-mottak.intern.dev.nav.no |
| qx | dev-fss | https://sokos-spk-mottak-qx.intern.dev.nav.no |
| prod | prod-fss | https://sokos-spk-mottak.intern.nav.no |

Manifestene ligger i [.nais](.nais).

## 4. Autentisering og autorisasjon

Applikasjonen bruker [Azure AD](https://doc.nais.io/auth/entra-id/) og validerer JWT-tokenet på hvert kall.
Hvilke applikasjoner som får kalle backend, står i `accessPolicy.inbound` i manifestene.

| Kaller | Token | Krav |
|---|---|---|
| `sokos-spk-mottak-admin` | OBO (på vegne av innlogget bruker) | Scope `spk-mottak.admin` og `NAVident` |
| `pensjon-pen` | M2M (maskin til maskin) | Rollen `leveattester.read` |

Brukere må være direkte medlem av AD-gruppen som er konfigurert for miljøet, ellers avviser Azure AD OBO-tokenet.

## 5. Deployment

Applikasjonen deployes med [GitHub Actions](https://github.com/navikt/sokos-spk-mottak/actions).

- Du kan ikke pushe direkte til `main`. Endringer må gå via en godkjent PR.
- Merge til `main` bygger og tester applikasjonen, deployer til q1 og qx, og deretter til prod.
- Du kan deploye manuelt til q1 eller qx med [manual-deploy-backend.yaml](../.github/workflows/manual-deploy-backend.yaml).

## 6. Drift og støtte

Jobber kan trigges fra [admin-dashboardet](../frontend/README.md).

Spørsmål om SFTP-tilkoblingen til SPK kan rettes til [#tech-linux](https://nav-it.slack.com/archives/CA2CM7QTX).

### Logging

Logger uten sensitive data går til [Grafana Loki](https://doc.nais.io/observability/logging/#grafana-loki).
Sensitive meldinger går til [Team Logs](https://doc.nais.io/observability/logging/how-to/team-logs/).

### Kubectl

For dev-fss:

```shell
kubectl config use-context dev-fss
kubectl get pods -n okonomi | grep sokos-spk-mottak
kubectl logs -f sokos-spk-mottak-<POD-ID> --namespace okonomi -c sokos-spk-mottak
```

For prod-fss:

```shell
kubectl config use-context prod-fss
kubectl get pods -n okonomi | grep sokos-spk-mottak
kubectl logs -f sokos-spk-mottak-<POD-ID> --namespace okonomi -c sokos-spk-mottak
```

### Alarmer

Applikasjonen bruker [Grafana Alerting](https://grafana.nav.cloud.nais.io/alerting/) til å overvåke HTTP-feilrater og
JVM-metrikker. Se [Nais-dokumentasjonen om alarmer](https://doc.nais.io/observability/alerts).

Varsler sendes til disse Slack-kanalene:

- Dev: [#utbetaling-team-beregning-alerts-dev](https://nav-it.slack.com/archives/C0BHK19HMPD)
- Prod: [#utbetaling-team-beregning-alerts-prod](https://nav-it.slack.com/archives/C0BUZH3FLLF)

### Grafana

- [sokos-spk-mottak](https://grafana.nav.cloud.nais.io/d/fdrtp6qv623ggf/sokos-spk-mottak?orgId=1&refresh=30s)

## 7. Henvendelser

Spørsmål om koden eller prosjektet kan stilles som issues her på GitHub.
Interne henvendelser kan sendes i Slack-kanalen [#utbetaling](https://nav-it.slack.com/archives/CKZADNFBP).
