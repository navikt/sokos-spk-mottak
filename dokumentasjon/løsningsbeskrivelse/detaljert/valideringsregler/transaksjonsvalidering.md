# Valideringsregler for transaksjoner

Alle transaksjoner må valideres etter følgende regler for å kunne godkjennes for overføring til T_TRANSAKSJON og videre til OppdragZ:

| Valideringsregel                     | Beskrivelse av kontroll                                                                                                                                                                        | Feilstatus |
|--------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------| 
| unik id                              | transaksjonId må ikke finnes hverken i T_TRANSAKSJON, T_AVV_TRANSAKSJON eller T_INN_TRANSAKSJON for anviser = SPK                                                                              | 01         |
| gyldig fødselsnummer                 | fødselsnummer må ligge i T_PERSON eller eksistere i PDL                                                                                                                                        | 02         |
| gyldig periode                       | utbetalingtransaksjon: fomDato angir første dag i måneden og tomDato angir siste dag i samme måned, trekktransaksjon: fomDato angir første dag i måneden og tomDato angir siste dag i en måned | 03         |
| gyldig beløpstype                    | beløpstype er 01 (skattepliktig utbetaling), 02 (ikke-skattepliktig utbetaling) eller 03 (trekk)                                                                                               | 04         |
| gyldig art                           | art eksisterer i T_K_ART                                                                                                                                                                       | 05         |
| gyldig anviser-dato                  | anviser-dato må ha gyldig format yyyymmdd                                                                                                                                                      | 09         |
| gyldig beløp                         | beløp må være angitt og > 0                                                                                                                                                                    | 10         |
| gyldig kombinasjon art og beløpstype | kominasjonen art og beløpstype må være gyldig i T_K_GYLDIG_KOMBIN                                                                                                                              | 11         |
| gyldig grad                          | grad må ha verdi dersom art er 'UFO', 'U67', 'AFP', 'UFE', 'UFT' eller 'ALP' og grad-verdi må være mellom 0 og 100                                                                             | 16         |

