# sokos-spk-mottak-admin

* [1. Funksjonelle krav](#1-funksjonelle-krav)
* [2. Arkitektur](#2-arkitektur)
* [3. Utviklingsmiljø](#3-utviklingsmiljø)
* [4. Autentisering](#4-autentisering)
* [5. Deployment](#5-deployment)
* [6. Drift og støtte](#6-drift-og-støtte)
* [7. Henvendelser](#7-henvendelser)

---

# 1. Funksjonelle krav

`sokos-spk-mottak-admin` er et frittstående admin-dashboard for [sokos-spk-mottak](../backend/README.md)-applikasjonen.
Kun utviklere og andre teammedlemmer (ikke saksbehandlere) skal ha tilgang. Herfra kan man blant annet trigge jobber
for lesing/validering av filer og sending av transaksjoner/oppdrag til Oppdrag Z.

Appen var tidligere en mikrofrontend lastet inn av Utbetalingsportalen, men kjører nå som en frittstående Vite-SPA
med egen URL og egen Nais-deployment.

# 2. Arkitektur

Appen består av to deler:

- **`src/`** – Vite + React SPA (klient). Se [vite.config.ts](vite.config.ts).
- **`server/`** – Express-server som:
  - serverer det bygde klient-buntet (`dist/`)
  - fungerer som en reverse proxy mot backend-API-et (`sokos-spk-mottak`)
  - bytter innkommende Azure AD-token til et OBO-token mot backend via [@navikt/oasis](https://github.com/navikt/oasis)
  - eksponerer `/internal/isAlive`, `/internal/isReady` og `/internal/metrics`

Se [server/src](server/src) for serverkoden, satt opp etter samme mønster som
[sokos-frivillig-skattetrekk-frontend](https://github.com/navikt/sokos-frivillig-skattetrekk-frontend/tree/main/server/src).

# 3. Utviklingsmiljø

### Forutsetninger

* Node (se `engines` i [package.json](package.json))
* pnpm (se `packageManager` i [package.json](package.json))

### Installere avhengigheter

```shell
pnpm install
cd server && pnpm install
```

### Kjøre lokalt

| Kommando | Beskrivelse |
|---|---|
| `pnpm run dev` | Starter Vite dev-server med [MSW](https://mswjs.io/) mock-data (`--mode mock`) |
| `pnpm run dev:backend` | Kjører mot lokal backend på `http://localhost:8080` (`--mode backend`) |
| `pnpm run dev:backend-q1` | Kjører mot backend i `q1`-miljøet (`--mode backend-q1`) |

For å kjøre serveren lokalt (Express + proxy):

```shell
cd server && pnpm run dev
```

> `NODE_ENV !== "production"` gjør at serveren returnerer et mock-OBO-token lokalt i stedet for å utføre en ekte
> token-utveksling, siden wonderwall/Azure AD-innlogging ikke er tilgjengelig utenfor Nais.

### Bygge

```shell
pnpm run build          # klient (tsc + vite build)
cd server && pnpm run build   # server (tsc --build)
```

### Miljøer

`sokos-spk-mottak-admin` kjøres i følgende miljøer:

- dev (dev-gcp)
- qx (dev-gcp)
- prod (prod-gcp)

# 4. Autentisering

Appen er beskyttet med [Azure AD](https://docs.nais.io/security/auth/azure-ad/) og bruker
[wonderwall](https://docs.nais.io/auth/sidecar/) (`azure.application.sidecar`) for automatisk innloggingsflyt.
Kun brukere i den konfigurerte AD-gruppen (se `.nais/*.yaml`) slipper inn.

Server-siden bytter det innkommende Azure AD-tokenet til et On-Behalf-Of-token mot backend
(`SOKOS_SPK_MOTTAK_BACKEND_AUDIENCE`) via `requestOboToken` fra `@navikt/oasis`, se
[server/src/token.ts](server/src/token.ts).

# 5. Deployment

Distribusjon av tjenesten er gjort med bruk av Github Actions.
[sokos-spk-mottak CI / CD](https://github.com/navikt/sokos-spk-mottak/actions)

Push/merge til main branch direkte er ikke mulig. Det må opprettes PR og godkjennes før merge til main branch.
Når PR er merged til main branch vil Github Actions bygge og deploye til dev-gcp og prod-gcp.
Har også mulighet for å deploye manuelt til `dev` eller `qx` via [manual-deploy-frontend.yaml](../.github/workflows/manual-deploy-frontend.yaml).

# 6. Drift og støtte

- [Spk Mottak Dashboard i dev](https://sokos-spk-mottak-admin.intern.dev.nav.no)
- [Spk Mottak Dashboard i prod](https://sokos-spk-mottak-admin.intern.nav.no)

### Kubectl

For dev-gcp:

```shell script
kubectl config use-context dev-gcp
kubectl get pods -n okonomi | grep sokos-spk-mottak-admin
kubectl logs -f sokos-spk-mottak-admin-<POD-ID> --namespace okonomi -c sokos-spk-mottak-admin
```

For prod-gcp:

```shell script
kubectl config use-context prod-gcp
kubectl get pods -n okonomi | grep sokos-spk-mottak-admin
kubectl logs -f sokos-spk-mottak-admin-<POD-ID> --namespace okonomi -c sokos-spk-mottak-admin
```

### Logging

Feilmeldinger og infomeldinger som ikke inneholder sensitive data logges til [Grafana Loki](https://docs.nais.io/observability/logging/#grafana-loki).

# 7. Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på Github.
Interne henvendelser kan sendes via Slack i kanalen [#utbetaling](https://nav-it.slack.com/archives/CKZADNFBP)
