# Valideringsregler for transaksjoner

Alle transaksjoner må valideres etter følgende regler for å kunne godkjennes for overføring til T_TRANSAKSJON og videre til OppdragZ:

* Validering av unik id på transaksjonene -> transaksjonId må ikke finnes hverken i T_TRANSAKSJON, T_AVV_TRANSAKSJON eller T_INN_TRANSAKSJON for anviser = SPK -> (feilkode: 01)
* Validering av gyldig fødselsnummer -> fødselsnummer må ligge i T_PERSON eller eksistere i PDL -> (feilkode: 02)
* Validering av gyldig periode, dvs fomDato og tomDato -> Dersom utbetalingtransaksjon skal fomDato angi første dag i måneden og tomDato angi siste dag i samme måned. Dersom trekktransaksjon skal fomDato angi første dag i måneden og tomDato angi siste dag i en måned (dvs. periode kan være større enn 1 måned) -> (feilkode: 03)
* Validering av gyldig beløpstype -> beløpstype er 01 (skattepliktig utbetaling), 02 (ikke-skattepliktig utbetaling) eller 03 (trekk) -> (feilkode: 04)
* Validering av gyldig art -> art eksisterer i kodeverkstabellen T_K_ART ->  (feilkode: 05)
* Validering av gyldig anviser-dato -> anviser-dato må ha gyldig format yyyymmdd -> (feilkode: 09)
* Validering av gyldig beløp -> beløp må være angitt og > 0 -> (feilkode: 10)
* Validering av gyldig kombinasjon art og beløpstype -> kominasjonen av art og beløpstype må være gyldig i T_K_GYLDIG_KOMBIN -> (feilkode: 11)
* Validering av gyldig grad -> grad må ha en verdi dersom art er en av følgende: 'UFO', 'U67', 'AFP', 'UFE', 'UFT', 'ALP' og grad-verdien må være mellom 0 og 100 -> (feilkode: 16)
