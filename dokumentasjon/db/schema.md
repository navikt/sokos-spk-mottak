# Transaksjonsskjema

Skapt fra DDL for produksjonsdatabasen 2026-10-07. Skjemaet omfatter de 11 tabellene som brukes av applikasjonen, og de 
14 tilhørende kodeverkstabellene. Historiske TEMP/FSDRIFT-tabeller, eldre batchtabeller og øvrige tabeller som ikke
brukes i kodebasen er utelatt.

Diagrammene er delt inn etter fagområde for å gjøre tekst og relasjoner lesbare. Oversikten viser bare tabellene 
som inngår i behandlingsflyten. Deldiagrammene viser alle deklarerte fremmednøkler og tilhørende kodeverkstabeller. 
Diagrammene viser bare primærnøkler (PK) og fremmednøkler (FK). Fullstendige kolonnedefinisjoner med datatyper, lengder, 
regler for NULL, identitetsinnstillinger, nøkler og indekser finnes i 
[`V1.0.2__create_transaction_schema.sql`](../../backend/src/main/resources/db/migration/V1.0.2__create_transaction_schema.sql).

Tabell- og kolonnenavn er skrevet med små bokstaver uten skjemaprefiks. PostgreSQL tolker navn med store bokstaver 
uten anførselstegn i applikasjonens SQL som de samme navnene. Db2-spesifikke innstillinger for tegnkoding, tabellområder, 
lagring og fysisk indeksoppsett er utelatt. Fremmednøklene opprettes etter tabellene for å håndtere den sirkulære 
avhengigheten mellom `t_transaksjon` og `t_trans_tilstand`. De kontrolleres ved hver SQL-setning og kan ikke utsettes 
til transaksjonen avsluttes.

Alle de 42 ikke-unike indeksene fra kilden er beholdt med opprinnelig kolonnerekkefølge. De unike indeksene fra kilden 
tilsvarer primærnøkler eller unikhetsregler og erstattes av disse. Db2-innstillingen `INCLUDE NULL KEYS` påvirker ikke 
unikhetsreglene for disse indeksene: Alle unike nøkler er enten primærnøkler eller unikhetsregelen for `t_person.fnr_fk`, 
som ikke tillater NULL. PostgreSQLs standardbehandling av NULL i unike nøkler gir derfor samme resultat.

Migreringen oppretter bare skjemaet. Den kopierer ikke data og endrer ikke applikasjonens tilkobling til Db2. Flyway 
kjøres ved oppstart av applikasjonen og oppretter tabellene i det konfigurerte PostgreSQL-skjemaet. Etter import av 
eksplisitte verdier i identitetskolonner må hver sekvens justeres slik at neste genererte verdi er større enn den høyeste 
importerte verdien.

## Oversikt over behandlingsflyten

Oversikten viser de sentrale tabellene i behandlingsflyten og bare primær- og fremmednøklene deres. Kodeverkstabellene 
og de separate tabellene `t_lopenr` og `t_avregning_avvik` er utelatt for å gjøre flyten tydeligere. Begge inngår i 
deldiagrammene nedenfor. `t_avregning_avvik` har ingen deklarerte fremmednøkler i kildens DDL.

```mermaid
erDiagram
    t_fil_info {
        int fil_info_id PK
    }
    t_inn_transaksjon {
        int inn_transaksjon_id PK
        int fil_info_id FK
    }
    t_avv_transaksjon {
        int avv_transaksjon_id PK
        int fil_info_id FK
    }
    t_transaksjon {
        int transaksjon_id PK
        int trans_tilstand_id FK
        int fil_info_id FK
        int person_id FK
    }
    t_person {
        int person_id PK
    }
    t_trans_tilstand {
        int trans_tilstand_id PK
        int transaksjon_id FK
    }
    t_retur_til_anv {
        int retur_til_anv_id PK
        int transaksjon_id FK
        int fil_info_ut_id FK
    }
    t_fil_info ||--o{ t_inn_transaksjon : fil_info_id
    t_fil_info ||--o{ t_avv_transaksjon : fil_info_id
    t_fil_info ||--o{ t_transaksjon : fil_info_id
    t_person ||--o{ t_transaksjon : person_id
    t_transaksjon o|--o{ t_trans_tilstand : transaksjon_id
    t_trans_tilstand o|--o{ t_transaksjon : trans_tilstand_id
    t_transaksjon o|--o{ t_retur_til_anv : transaksjon_id
    t_fil_info o|--o{ t_retur_til_anv : fil_info_ut_id
```

## Filinnlasting og validering

`t_inn_transaksjon` og `t_avv_transaksjon` er knyttet til filtabellen og kodeverket for transaksjonsstatus. `t_lopenr` 
har bare avhengigheter til kodeverkstabeller. Kolonnen `t_retur_til_anv.fil_info_inn_id` for innkommende fil er ikke 
deklarert som fremmednøkkel i kilden og vises derfor ikke som en relasjon.

```mermaid
erDiagram
    t_k_anviser {
        string k_anviser PK
    }
    t_k_fil_s {
        string k_fil_s PK
    }
    t_k_fil_t {
        string k_fil_t PK
    }
    t_k_fil_tilstand_t {
        string k_fil_tilstand_t PK
    }
    t_k_transaksjon_s {
        string k_transaksjon_s PK
    }
    t_fil_info {
        int fil_info_id PK
        string k_fil_s FK
        string k_anviser FK
        string k_fil_t FK
        string k_fil_tilstand_t FK
    }
    t_inn_transaksjon {
        int inn_transaksjon_id PK
        int fil_info_id FK
        string k_transaksjon_s FK
    }
    t_avv_transaksjon {
        int avv_transaksjon_id PK
        int fil_info_id FK
        string k_transaksjon_s FK
    }
    t_lopenr {
        int lopenr_id PK
        string k_fil_t FK
        string k_anviser FK
    }
    t_k_anviser o|--o{ t_fil_info : k_anviser
    t_k_fil_s ||--o{ t_fil_info : k_fil_s
    t_k_fil_t ||--o{ t_fil_info : k_fil_t
    t_k_fil_tilstand_t o|--o{ t_fil_info : k_fil_tilstand_t
    t_fil_info ||--o{ t_inn_transaksjon : fil_info_id
    t_k_transaksjon_s o|--o{ t_inn_transaksjon : k_transaksjon_s
    t_fil_info ||--o{ t_avv_transaksjon : fil_info_id
    t_k_transaksjon_s ||--o{ t_avv_transaksjon : k_transaksjon_s
    t_k_fil_t o|--o{ t_lopenr : k_fil_t
    t_k_anviser o|--o{ t_lopenr : k_anviser
```

## Transaksjoner, personer og tilstandshistorikk

De to relasjonene mellom `t_transaksjon` og `t_trans_tilstand` er separate fremmednøkler i hver sin retning. Begge 
tillater NULL. Den ene knytter historikkrader til transaksjonen, mens den andre lar transaksjonen peke på gjeldende 
tilstandsrad. Begge er deklarert i kilden.

```mermaid
erDiagram
    t_fil_info {
        int fil_info_id PK
    }
    t_person {
        int person_id PK
    }
    t_k_anviser {
        string k_anviser PK
    }
    t_k_art {
        string k_art PK
    }
    t_k_belop_t {
        string k_belop_t PK
    }
    t_k_transaksjon_s {
        string k_transaksjon_s PK
    }
    t_k_trans_tolkning {
        string k_trans_tolkning PK
    }
    t_k_trekkansvar {
        string k_trekkansvar PK
    }
    t_k_trans_tilst_t {
        string k_trans_tilst_t PK
    }
    t_transaksjon {
        int transaksjon_id PK
        int trans_tilstand_id FK
        int fil_info_id FK
        int person_id FK
        string k_transaksjon_s FK
        string k_belop_t FK
        string k_art FK
        string k_anviser FK
        string k_trans_tolkning FK
        string k_trekkansvar FK
        string k_trans_tilst_t FK
    }
    t_trans_tilstand {
        int trans_tilstand_id PK
        int transaksjon_id FK
        string k_trans_tilst_t FK
    }
    t_fil_info ||--o{ t_transaksjon : fil_info_id
    t_person ||--o{ t_transaksjon : person_id
    t_k_transaksjon_s ||--o{ t_transaksjon : k_transaksjon_s
    t_k_belop_t ||--o{ t_transaksjon : k_belop_t
    t_k_art ||--o{ t_transaksjon : k_art
    t_k_anviser ||--o{ t_transaksjon : k_anviser
    t_k_trans_tolkning o|--o{ t_transaksjon : k_trans_tolkning
    t_k_trekkansvar o|--o{ t_transaksjon : k_trekkansvar
    t_k_trans_tilst_t o|--o{ t_transaksjon : k_trans_tilst_t
    t_trans_tilstand o|--o{ t_transaksjon : trans_tilstand_id
    t_transaksjon o|--o{ t_trans_tilstand : transaksjon_id
    t_k_trans_tilst_t o|--o{ t_trans_tilstand : k_trans_tilst_t
```

## Retur og avstemming

Bare `fil_info_ut_id`, ikke `fil_info_inn_id`, refererer til `t_fil_info` i kildens DDL. `t_avregning_avvik` er en 
selvstendig tabell i det valgte skjemaet og har ingen deklarerte fremmednøkler.

```mermaid
erDiagram
    t_fil_info {
        int fil_info_id PK
    }
    t_transaksjon {
        int transaksjon_id PK
    }
    t_k_anviser {
        string k_anviser PK
    }
    t_k_retur_t {
        string k_retur_t PK
    }
    t_retur_til_anv {
        int retur_til_anv_id PK
        string k_retur_t FK
        string k_anviser FK
        int transaksjon_id FK
        int fil_info_ut_id FK
    }
    t_avregning_avvik {
        int avregning_avvik_id PK
    }
    t_fil_info o|--o{ t_retur_til_anv : fil_info_ut_id
    t_transaksjon o|--o{ t_retur_til_anv : transaksjon_id
    t_k_anviser ||--o{ t_retur_til_anv : k_anviser
    t_k_retur_t o|--o{ t_retur_til_anv : k_retur_t
```

## Kodeverk for gyldige kombinasjoner

Dette diagrammet viser de sju kodeverkstabellene som `t_k_gyldig_kombin` refererer til.

```mermaid
erDiagram
    t_k_anviser {
        string k_anviser PK
    }
    t_k_art {
        string k_art PK
    }
    t_k_belop_t {
        string k_belop_t PK
    }
    t_k_fagomrade {
        string k_fagomrade PK
    }
    t_k_trekk_t {
        string k_trekk_t PK
    }
    t_k_trekkalt_t {
        string k_trekkalt_t PK
    }
    t_k_trekkgruppe {
        string k_trekkgruppe PK
    }
    t_k_gyldig_kombin {
        int k_gyldig_kombin_id PK
        string k_art FK
        string k_belop_t FK
        string k_trekkgruppe FK
        string k_trekk_t FK
        string k_trekkalt_t FK
        string k_anviser FK
        string k_fagomrade FK
    }
    t_k_anviser ||--o{ t_k_gyldig_kombin : k_anviser
    t_k_art ||--o{ t_k_gyldig_kombin : k_art
    t_k_belop_t ||--o{ t_k_gyldig_kombin : k_belop_t
    t_k_fagomrade o|--o{ t_k_gyldig_kombin : k_fagomrade
    t_k_trekk_t o|--o{ t_k_gyldig_kombin : k_trekk_t
    t_k_trekkalt_t o|--o{ t_k_gyldig_kombin : k_trekkalt_t
    t_k_trekkgruppe o|--o{ t_k_gyldig_kombin : k_trekkgruppe
```

## Tabell- og kolonneoversikt

Alle de 25 tabellene og 359 kolonnene finnes i migreringen som er lenket ovenfor. Deldiagrammene viser nøklene som 
trengs for å følge relasjonene, uten å gjenta beskrivende kolonner i hvert diagram.

| Fagområde | Tabeller |
| --- | --- |
| Filinnlasting og validering | `t_fil_info`, `t_inn_transaksjon`, `t_avv_transaksjon`, `t_lopenr`, `t_k_fil_s`, `t_k_fil_t`, `t_k_fil_tilstand_t`, `t_k_transaksjon_s` |
| Transaksjoner, personer og tilstandshistorikk | `t_transaksjon`, `t_person`, `t_trans_tilstand`, `t_k_art`, `t_k_belop_t`, `t_k_anviser`, `t_k_trans_tolkning`, `t_k_trekkansvar`, `t_k_trans_tilst_t` |
| Retur og avstemming | `t_retur_til_anv`, `t_avregning_avvik`, `t_k_retur_t` |
| Kodeverk for gyldige kombinasjoner | `t_k_gyldig_kombin`, `t_k_fagomrade`, `t_k_trekk_t`, `t_k_trekkalt_t`, `t_k_trekkgruppe` |

Noen kodeverkstabeller vises i flere deldiagrammer fordi flere tabeller refererer til dem. Til sammen dekker diagrammene 
alle de 25 tabellene og de 33 fremmednøklene som er deklarert i kilden.
