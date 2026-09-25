# sokos-spk-mottak

Monorepo med backend og admin-frontend for SPK-mottak. Løsningen mottar utbetalings- og trekktransaksjoner fra
Statens pensjonskasse (SPK) og sender dem til Oppdrag Z.

| Mappe | Beskrivelse | README |
|---|---|---|
| [`backend/`](backend) | Kotlin/Ktor-applikasjon som mottar utbetalings- og trekktransaksjoner fra SPK og sender oppdrag til Oppdrag Z | [backend/README.md](backend/README.md) |
| [`frontend/`](frontend) | Vite/React admin-dashboard for å trigge jobber i backend manuelt | [frontend/README.md](frontend/README.md) |

## Funksjonelle krav

### Backend

`sokos-spk-mottak` mottar utbetalings- og trekktransaksjoner fra Statens pensjonskasse (SPK) og sender utbetalings- og
trekkoppdrag til Oppdrag Z.

### Frontend

`sokos-spk-mottak-admin` er et admin-dashboard for de som drifter backend. Herfra kan du trigge jobber for lesing og
validering av filer og sending av transaksjoner og oppdrag til Oppdrag Z.

Du må være medlem av AD-gruppen som er konfigurert for miljøet for å få tilgang til frontend.

## Slik henger appene sammen

```
Drift ─► sokos-spk-mottak-admin ─(OBO-token)─► sokos-spk-mottak ─► SFTP, DB2, MQ, Oppdrag Z
                                                       ▲
pensjon-pen ─(M2M-token)───────────────────────────────┘
```

- De som drifter løsningen, kan trigge jobber manuelt i frontend ved behov. Frontend kaller backend på vegne av den som er
  logget inn, med et OBO-token.
- `pensjon-pen` henter leveattester fra backend med et M2M-token.

Se [autentisering i backend](backend/README.md#4-autentisering-og-autorisasjon) for detaljer.

## Struktur

```
sokos-spk-mottak/
├── backend/     # Kotlin/Ktor, Gradle-prosjekt, kjører i dev-fss/prod-fss
├── frontend/    # Vite/React-SPA og Express-server, kjører i dev-gcp/prod-gcp
└── .github/     # GitHub Actions-workflows for hver app
```

Backend og frontend er uavhengige applikasjoner som deployes hver for seg, men deler samme repo og
CI/CD-konvensjoner fra [mob-gha-workflows](https://github.com/navikt/mob-gha-workflows).

## Utviklingsoppsett

Backend (Kotlin/Gradle) og frontend (Node/pnpm) har hver sin verktøykjede. Åpne dem som egne prosjekter i IDE-en,
for eksempel `backend/` i IntelliJ og `frontend/` i VS Code eller WebStorm.

Se README-en for hver app for oppsett, kommandoer og miljøer.

## CI/CD

Workflowene i [.github/workflows](.github/workflows) finnes i en `-backend`- og en `-frontend`-variant. Hver workflow
kjører bare når filer i sin app endres (se `paths-ignore`). En PR som bare endrer frontend, bygger og tester derfor ikke
backend, og omvendt.

Merge til `main` deployer appen til q1 og qx, og deretter til prod. Se Deployment i README-en for hver app.

## Henvendelser

Spørsmål om koden eller prosjektet kan stilles som issues her på GitHub.
Interne henvendelser kan sendes i Slack-kanalen [#utbetaling](https://nav-it.slack.com/archives/CKZADNFBP).
