# sokos-spk-mottak

Monorepo som inneholder både backend og admin-frontend for SPK Mottak.

| Mappe | Beskrivelse | README |
|---|---|---|
| [`backend/`](backend) | Kotlin/Ktor-applikasjon som mottar utbetalings- og trekktransaksjoner fra SPK og sender oppdrag til Oppdrag Z | [backend/README.md](backend/README.md) |
| [`frontend/`](frontend) | `sokos-spk-mottak-admin` – frittstående Vite/React admin-dashboard for å trigge og overvåke jobber i backend | [frontend/README.md](frontend/README.md) |

## Struktur

```
sokos-spk-mottak/
├── backend/     # Kotlin/Ktor, Gradle-prosjekt, kjører i dev-fss/prod-fss
├── frontend/    # Vite/React SPA + Express server, kjører i dev-gcp/prod-gcp
└── .github/     # Delte og app-spesifikke GitHub Actions workflows
```

Backend og frontend er uavhengige applikasjoner som deployes hver for seg, men deler samme repo og
CI/CD-konvensjoner fra [mob-gha-workflows](https://github.com/navikt/mob-gha-workflows).

## Utviklingsoppsett

Backend (Kotlin/Gradle) og frontend (Node/pnpm) har separate verktøykjeder og bør normalt åpnes som
egne prosjekter i IDE-en din (f.eks. `backend/` i IntelliJ, `frontend/` i VS Code/WebStorm), fremfor
å prøve å syncronisere begge i ett og samme vindu.

Se den respektive README-en for detaljert oppsett, kommandoer og miljøvariabler.

## CI/CD

Alle workflows i [.github/workflows](.github/workflows) er delt inn i `-backend`- og `-frontend`-varianter,
og trigges kun når relevante filer for den aktuelle appen endres (se `paths-ignore` i hver workflow).
Dette gjør at en PR som kun endrer frontend ikke trigger bygg/test av backend, og omvendt.

## Henvendelser

Spørsmål knyttet til koden eller prosjektet kan stilles som issues her på Github.
Interne henvendelser kan sendes via Slack i kanalen [#utbetaling](https://nav-it.slack.com/archives/CKZADNFBP)
