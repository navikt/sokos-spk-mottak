# Kjente feil

Det har oppstått et par ganger at enkelt transaksjoner feiler med feilmeldingen **Prosessering av returmelding feilet med alvorlighetsgrad 08.**
Dette indikerer at Oppdrag Z ikke har akseptert oppdragsmeldingen og at den har returnert en respons som har verdien **ORF** i sokos-spk-mottak.

For å finmne årsaken til denne feilen, logg inn i databasen og kjør følgende query:
```sql
set schema MO611P;
select * from t_trans_tilstand where transaksjon_id in (select transaksjon_id from t_transaksjon where k_trans_tilst_t = 'ORF' and dato_endret > CURRENT_TIMESTAMP - 2 DAYS);
```
Kolonnen **feilkodemelding** inneholder meldingen som forklarer hva slags type feil det er. Hvis det står **OPPDRAGET/FAGSYSTEM-ID finnes ikke fra før** er det en feil hvor **trans_tolkning**-tagen i oppdragsmeldingen er feil.
Dette kan verifiseres ved å sjekke kolonnen **k_trans_tolkning** i **t_transaksjon**
```sql
select * from t_transaksjon where k_trans_tilst_t = 'ORF' and dato_endret > CURRENT_TIMESTAMP - 2 DAYS;
```
Dersom **k_trans_tolkning**-kolonnen har verdien **NY_EKSIST** er det en feil som beskrevet i TOB-sak https://jira.adeo.no/browse/TOB-6441

Finn kolonnene **trans_eks_id_fk** og **fnr_fk** for transaksjonen som feilet og følg instruksene beskrevet i denne TOB-saken slik at de feilede transaksjonene blir behandlet riktig.

For å verifisere at alt er ok igjen, kjør følgene query til slutt som skal gi svaret 0:
```sql
select count(*) from t_transaksjon where k_trans_tilst_t = 'ORF' and dato_endret > CURRENT_TIMESTAMP - 1 DAYS;
```