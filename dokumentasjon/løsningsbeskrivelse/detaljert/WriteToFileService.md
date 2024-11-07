# WriteToFileService
Tjenesten produserer en returfil for hver anvisningsfil som er mottat fra SPK. Denne filen har samme format og innhold som anvisningsfilen men har i tillegg lagt inn informasjon om transaksjonenes valideringstatus.

**Startbetingelse:** Dersom T_INN_TRANSAKSJON inneholder transaksjoner som er ferdigbehandlet, dvs kolonnen BEHANDLET i T_INN_TRANSAKSJON er J.

For hver anvisningsfil som det ikke er blitt produsert returfil for:
* Produserer en returfil som inneholder samme innhold som anvisningsfil, men med valideringstatus per transaksjon i tillegg. Filnavn = SPK_NAV_\<tidspunkt>_ANV hvor tidspunkt = yyyyMMdd_HHmmss
* Oppretter en record i T_FIL_INFO med informasjon om returfilen slik som filnavn, filtype = INL (innlesningsretur) og filtilstandstatus = RET (returnert).
* Fjerner alle innslagene i T_INN_TRANSAKSJON for anvisningsfilen.

Ved feil i behandlingen stoppes tjenesten, men returfiler som allerede er behandlet og lagt på SFTP-serveren blir ikke reprodusert når tjenesten re-kjøres.
