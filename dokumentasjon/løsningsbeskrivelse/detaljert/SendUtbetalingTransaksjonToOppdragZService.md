# SendUtbetalingTransaksjonToOppdragZService
Tjenesten er ansvarlig for å sende alle utbetalingstransaksjoner til Oppdragssystemet (OS) som ligger i T_TRANSAKSJON og som ikke er oversendt ennå eller har feilet ved tidligere oversendelsesforsøk.

**Startbetingelse**: T_INN_TRANSAKSJON er tom.

Først hentes alle transaksjoner som har en av følgende statuser: 
* opprettet (OPR)
* feil ved sending (OSF)
* manuelt korrigert (MKR)

Transaksjonene grupperes på personId og fagområde før de konverteres til en utbetalingsmelding. Dette vil føre til at transaksjoner som er knyttet til samme person og fagomåde kommer i samme melding. 
Deretter oversendes meldingene batchvis til en MQ-kø på xml-format.

For hver meldingsbatch-operasjon mot kø blir T_TRANSAKSJON og T_TRANS_TILSTAND oppdatert med riktig status for de transaksjoner som inngår i meldingene
Dersom db-operasjonene skulle feile, vil status til transaksjonene i T_TRANSAKSJON og T_TRANS_TILSTAND forbli uforandret, dvs opprettet (OPR), og følgelig vil de oversendes til kø neste gang tjenesten kjører.
Dette kan medføre duplikater i OS, men dette vil bli håndtert i mottak av reply-meldinger fra OS.

Løsningen unngår derfor den uønskede tilstanden at db viser ok-status mens meldingene ikke har blit sendt til køen med suksess,

Når oversendelsen er ferdig, oppdateres T_FIL_INFO med K_AVSTEMMING_S lik OSO (Oppdrag Sendt Ok) for de anvisningsfilene som inneholdt de prosesserte utbetalingstransaksjonene.

**Mapping av utbetalingsmeldinger**
