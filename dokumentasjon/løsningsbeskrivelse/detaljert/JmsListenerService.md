# JmsListenerService
Tjenesten lytter til reply-meldinger som sendes fra OS og lagrer nødvendig meldingsinformasjon i T_TRANSAKSJON og T_TRANS_TILSTAND. 
Reply-meldingene inneholder informasjon om status til behandlingen av transaksjonene i OS.

For hver melding sjekkes statusinformasjonen (**alvorlighetsgrad**) og dersom 
* alvorlighetsgrad = 12, logges en feilmelding og feilstatus lagres (pkt. under)
* alvorlighetsgrad > 4 lagres status **ORF** (Oppdrag Retur Feil hvis utbetalingsmelding) eller **TRF** (Trekk Retur Feil hvis trekkmelding) i T_TRANSAKSJON og T_TRANS_TILSTAND
* alvorlighetsgrad < 4 lagres status **ORO** (Oppdrag Retur Ok hvis utbetalingsmelding) eller **TRO** (Trekk Retur Ok hvis trekkmelding) i T_TRANSAKSJON og T_TRANS_TILSTAND

Videre sjekkes det om meldingen er en duplikatmelding. Duplikatsjekken gjøres på **transaksjonsId** i trekkmeldinger og **delytelseId** i utbetalingsmeldinger.
1. Dersom alvorlighetsgrad = "00" (status ok) bypasses duplikatsjekken og status **ORO** eller **TRO** lagres.
2. Dersom alvorlighetsgrad != "00" og transaksjonen eksisterer i T_TRANSAKSJON med status **ORO** eller **TRO**, blir ikke transaksjonens status oppdatert.
3. Dersom hverken 1 eller 2, blir transaksjonens status oppdatert.

I T_TRANS_TILSTAND blir det opprettet en ny rad hvor kolonnene **feilkode** og **feilkodemelding** tildeles verdi fra henholdsvis **kodeMelding** og **beskrMelding** fra OS-meldingen.
I T_TRANSAKSJON blir kolonnen **os_status** oppdatert med **alvorlighetsgrad** fra OS-meldingen, og dersom det er en trekkmelding blir i tillegg kolonnen **trekkvedtak_id_fk** oppdatert fra OS-meldingen.

