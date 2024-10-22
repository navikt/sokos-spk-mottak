# SendTrekkTransaksjonToOppdragZService

Tjenesten er ansvarlig for å sende alle trekktransaksjoner til Oppdragssystemet(OS) som ligger i T_TRANSAKSJON og som ikke er oversendt ennå eller har feilet ved tidligere oversendelsesforsøk.

**Startbetingelse:** T_INN_TRANSAKSJON er tom.

Først hentes alle transaksjoner som har en av følgende statuser:

* opprettet (OPR)
* feil ved sending (TSF)
* manuelt korrigert (MKR)

Meldingene overføres batchvis til en MQ-kø på json-format.

For hver meldingsbatch-operasjon mot kø blir T_TRANSAKSJON og T_TRANS_TILSTAND oppdatert med riktig status for de transaksjoner som inngår i meldingene.
Dersom db-operasjonene skulle feile, vil status til transaksjonene i T_TRANSAKSJON og T_TRANS_TILSTAND forbli uforandret, dvs opprettet (OPR), og følgelig vil de oversendes til kø neste gang tjenesten
kjører.
Dette kan resultere i duplikater i OS, men dette vil bli håndtert i mottaket av reply-meldinger fra OS.

Løsningen unngår derfor den uønskede tilstanden at db viser ok-status mens meldingene ikke har blitt sendt suksessfult til køen.

**Mapping av trekkmeldinger**

dokument.transaksjonsId = T_TRANSAKSJON.transaksjon_id
<br/>&emsp;dokument.innrapporteringTrekk.aksjonskode = "NY"
<br/>&emsp;dokument.innrapporteringTrekk.kreditorIdTss = "80000427901"
<br/>&emsp;dokument.innrapporteringTrekk.kreditorTrekkId = T_TRANSAKSJON.trans_eks_id_fk
<br/>&emsp;dokument.innrapporteringTrekk.debitorId = T_TRANSAKSJON.fnr_fk
<br/>&emsp;dokument.innrapporteringTrekk.kodeTrekktype = T_K_GYLDIG_KOMBIN.k_trekk_t
<br/>&emsp;dokument.innrapporteringTrekk.kodeTrekkAlternativ = T_K_GYLDIG_KOMBIN.k_trekkalt_t
<br/>&emsp;&emsp;dokument.innrapporteringTrekk.perioder.periode.periodeFomDato = T_TRANSAKSJON.dato_fom
<br/>&emsp;&emsp;dokument.innrapporteringTrekk.perioder.periode.periodeTomDato = T_TRANSAKSJON.dato_tom
<br/>&emsp;&emsp;dokument.innrapporteringTrekk.perioder.periode.sats = T_TRANSAKSJON.belop/100

Eksempel:

```json
{
	"dokument": {
		"transaksjonsId": "82207456",
		"innrapporteringTrekk": {
			"aksjonskode": "NY",
			"kreditorIdTss": "80000427901",
			"kreditorTrekkId": "115371378",
			"debitorId": "01093340562",
			"kodeTrekktype": "SPK1",
			"kodeTrekkAlternativ": "LOPM",
			"perioder": {
				"periode": [
					{
						"periodeFomDato": "2023-11-01",
						"periodeTomDato": "2023-11-30",
						"sats": 125.0
					}
				]
			}
		}
	}
}
