# Tester for validering av transaksjoner

| Valideringsregel                     | Beskrivelse av test                                                                                     | Feilstatus |
|--------------------------------------|---------------------------------------------------------------------------------------------------------|------------| 
| unik id                              | T_INN_TRANSAKSJON har to transaksjoner med lik TRANS_ID_FK                                              | 01         |
| unik id                              | T_INN_TRANSAKSJON har en transaksjon med samme TRANS_ID_FK som en i T_AVV_TRANSAKSJON og anviser er lik | 01         |
| unik id                              | T_INN_TRANSAKSJON har en transaksjon med samme TRANS_ID_FK som en i T_TRANSAKSJON og anviser er lik     | 01         |
| gyldig fødselsnummer                 | T_INN_TRANSAKSJON har en transaksjon med FNR_FK som ikke eksisterer i T_PERSON eller i PDL              | 02         |
| gyldig periode                       | T_INN_TRANSAKSJON har en transaksjon med BELOPSTYPE 01 og DATO_FOM er ikke første dag i måneden         | 03         |
| gyldig periode                       | T_INN_TRANSAKSJON har en transaksjon med BELOPSTYPE 01 og DATO_TOM er siste dag i neste måned           | 03         |
| gyldig periode                       | T_INN_TRANSAKSJON har en transaksjon med BELOPSTYPE 03 og DATO_FOM er ikke første dag i måneden         | 03         |
| gyldig periode                       | T_INN_TRANSAKSJON har en transaksjon med BELOPSTYPE 03 og DATO_TOM er ikke siste dag i måneden          | 03         |
| gyldig beløpstype                    | T_INN_TRANSAKSJON har en transaksjon med ugyldig BELOPSTYPE = 04                                        | 04         |
| gyldig art                           | T_INN_TRANSAKSJON har en transaksjon med ugyldig ART = XXX                                              | 05         |
| gyldig anviser-dato                  | T_INN_TRANSAKSJON har en transaksjon med ugyldig DATO_ANVISER = null                                    | 09         |
| gyldig beløp                         | T_INN_TRANSAKSJON har en transaksjon med ugyldig BELOP = 0                                              | 10         |
| gyldig kombinasjon art og beløpstype | T_INN_TRANSAKSJON har en transaksjon med ugyldig kombinasjon av ART = ETT og BELOPSTYPE = 03            | 11         |
| gyldig grad                          | T_INN_TRANSAKSJON har en transaksjon med ART = U67  og GRAD = null                                      | 16         |
| gyldig grad                          | T_INN_TRANSAKSJON har en transaksjon med ART = U67  og GRAD = -20                                       | 16         |
| gyldig grad                          | T_INN_TRANSAKSJON har en transaksjon med ART = U67  og GRAD = 200                                       | 16         |

