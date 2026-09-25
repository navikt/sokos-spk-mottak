# sokos-spk-mottak-admin

Vite/React admin-dashboard for å trigge jobber manuelt i [sokos-spk-mottak](../backend/README.md).

* [1. Arkitektur](#1-arkitektur)
* [2. Utviklingsmiljø](#2-utviklingsmiljø)
* [3. Miljøer](#3-miljøer)
* [4. Autentisering og autorisasjon](#4-autentisering-og-autorisasjon)
* [5. Deployment](#5-deployment)
* [6. Drift og støtte](#6-drift-og-støtte)
* [7. Henvendelser](#7-henvendelser)

---

## 1. Arkitektur

Appen består av to deler:

- **`src/`** er klienten, en Vite/React-app. Se [vite.config.ts](vite.config.ts).
- **`server/`** er en Express-server som
  - serverer den bygde klienten fra `dist/`
  - bytter brukerens Azure AD-token til et OBO-token og kaller backend med det
  - eksponerer helsesjekker og metrikker for Nais

## 2. Utviklingsmiljø

### Forutsetninger

* Node (se `engines` i [package.json](package.json))
* pnpm (se `packageManager` i [package.json](package.json))

### Installere avhengigheter

Klienten og serveren har hver sin `package.json` og installeres hver for seg:

```shell
pnpm install
cd server && pnpm install
```

### Bygge

```shell
pnpm run build                # klient (tsc + vite build)
cd server && pnpm run build   # server (tsc --build)
```

### Kjøre lokalt

| Kommando | Beskrivelse |
|---|---|
| `pnpm run dev` | Starter Vite med mock-data fra [MSW](https://mswjs.io/) |
| `pnpm run dev:backend` | Kjører mot lokal backend på `http://localhost:8080` |
| `pnpm run dev:backend-q1` | Kjører mot backend i q1 |

Slik kjører du Express-serveren lokalt:

```shell
cd server && pnpm run dev
```

Utenfor Nais finnes ikke Azure AD-innloggingen. Når `NODE_ENV` ikke er `production`, bruker serveren derfor et
mock-token i stedet for å hente et ekte OBO-token.

## 3. Miljøer

| Miljø | Cluster | URL |
|---|---|---|
| q1 | dev-gcp | https://sokos-spk-mottak-admin.intern.dev.nav.no |
| qx | dev-gcp | https://sokos-spk-mottak-admin-qx.intern.dev.nav.no |
| prod | prod-gcp | https://sokos-spk-mottak-admin.intern.nav.no |

Manifestene ligger i [.nais](.nais).

## 4. Autentisering og autorisasjon

Appen bruker [Azure AD](https://doc.nais.io/auth/entra-id/) med
[innlogging via sidecar](https://doc.nais.io/auth/entra-id/how-to/login/) (`azure.sidecar`). Brukere som ikke er
logget inn, sendes automatisk til innlogging.

Du må være direkte medlem av AD-gruppen som er konfigurert for miljøet i `.nais/*.yaml`, for å få tilgang.

Serveren bytter brukerens token til et OBO-token for backend (`SOKOS_SPK_MOTTAK_BACKEND_AUDIENCE`) med
`requestOboToken` fra [@navikt/oasis](https://github.com/navikt/oasis). Se [server/src/token.ts](server/src/token.ts).

## 5. Deployment

Appen deployes med [GitHub Actions](https://github.com/navikt/sokos-spk-mottak/actions).

- Du kan ikke pushe direkte til `main`. Endringer må gå via en godkjent PR.
- Merge til `main` bygger appen, deployer til q1 og qx, og deretter til prod.
- Du kan deploye manuelt til q1 eller qx med [manual-deploy-frontend.yaml](../.github/workflows/manual-deploy-frontend.yaml).

## 6. Drift og støtte

### Logging

Logger uten sensitive data går til [Grafana Loki](https://doc.nais.io/observability/logging/#grafana-loki).
Logglinjene har `trace_id` og `span_id`, slik at du kan koble dem til traces i Nais APM.

### Kubectl

For dev-gcp:

```shell
kubectl config use-context dev-gcp
kubectl get pods -n okonomi | grep sokos-spk-mottak-admin
kubectl logs -f sokos-spk-mottak-admin-<POD-ID> --namespace okonomi -c sokos-spk-mottak-admin
```

For prod-gcp:

```shell
kubectl config use-context prod-gcp
kubectl get pods -n okonomi | grep sokos-spk-mottak-admin
kubectl logs -f sokos-spk-mottak-admin-<POD-ID> --namespace okonomi -c sokos-spk-mottak-admin
```

## 7. Henvendelser

Spørsmål om koden eller prosjektet kan stilles som issues her på GitHub.
Interne henvendelser kan sendes i Slack-kanalen [#utbetaling](https://nav-it.slack.com/archives/CKZADNFBP).
