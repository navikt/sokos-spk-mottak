# SendTrekkTransaksjonToOppdragZService
Tjenesten er ansvarlig for å sende alle trekktransaksjoner til Oppdragssystemet(OS) som ligger i T_TRANSAKSJON og som ikke er oversendt ennå eller har feilet ved tidligere oversendelsesforsøk.

**Startbetingelse:** T_INN_TRANSAKSJON er tom.

Først hentes alle transaksjoner som har en av følgende statuser:
* opprettet (OPR)
* feil ved sending (TSF)
* manuelt korrigert (MKR)

Meldingene overføres batchvis til en MQ-kø på json-format.

For hver meldingsbatch-operasjon mot kø blir T_TRANSAKSJON og T_TRANS_TILSTAND oppdatert med riktig status for de transaksjoner som inngår i meldingene
Dersom db-operasjonene skulle feile, vil status til transaksjonene i T_TRANSAKSJON og T_TRANS_TILSTAND forbli uforandret, dvs opprettet (OPR), og følgelig vil de oversendes til kø neste gang tjenesten kjører.
Dette kan medføre duplikater i OS, men dette vil bli håndtert i mottak av reply-meldinger fra OS.

Løsningen unngår derfor den uønskede tilstanden at db viser ok-status mens meldingene ikke har blit sendt til køen med suksess,

**Mapping av trekkmeldinger**

dokument.transaksjonsId = T_TRANSAKSJON.transaksjon_id
dokument.innrapporteringTrekk.aksjonskode = "NY"
dokument.innrapporteringTrekk.kreditorIdTss = "80000427901"
dokument.innrapporteringTrekk.kreditorTrekkId = T_TRANSAKSJON.trans_eks_id_fk
dokument.innrapporteringTrekk.debitorId = T_TRANSAKSJON.fnr_fk
dokument.innrapporteringTrekk.kodeTrekktype = T_K_GYLDIG_KOMBIN.K_TREKK_T
dokument.innrapporteringTrekk.kodeTrekkAlternativ = T_K_GYLDIG_KOMBIN.K_TREKKALT_T
dokument.innrapporteringTrekk.perioder.periode.periodeFomDato = T_TRANSAKSJON.dato_fom
dokument.innrapporteringTrekk.perioder.periode.periodeTomDato = T_TRANSAKSJON.dato_tom
dokument.innrapporteringTrekk.perioder.periode.sats = T_TRANSAKSJON.belop/100
