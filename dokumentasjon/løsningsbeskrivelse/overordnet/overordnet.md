# Overordnet beskrivelse

NAV gjennomfører utbetaling av ytelser og trekk på vegne av SPK og mottar daglig filer med transaksjoner som skal behandles i økonomiløsningen. Transaksjonene blir validert og lastet inn i den nye mottakskomponenten `sokos-spk-mottak` og deretter overført til Oppdragssystemet (OS) for videre behandling i økonomiløsningen. Videre mottar `sokos-spk-mottak` daglig meldinger med grunnlagsdata som benyttes for å utføre avregninger som sendes som filer til SPK.
`sokos-spk-mottak` består av følgende tjenester:

1. [ReadAndParseFileService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/ReadAndParseFileService.kt)
<br/> Tjeneste som leser inn ubehandled(e) fil(er) med transaksjoner fra SPK og validerer formatet på filen. Dersom filen aksepteres, lagres innholdet i en midlertidig innlastingstabell, `T_INN_TRANSAKSJON`. Dersom den ikke aksepteres, lagres ikke innholdet og det produseres en returfil som beskriver feilen og som sendes tilbake til SPK.
   
2. [ValidateTransaksjonService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/ValidateTransaksjonService.kt) 
<br/> Tjeneste som behandler transaksjonene som er lastet inn i innlastingstabellen i forrige trinn. Dette består av validering av transaksjonene som ligger i tabellen og lagre disse permanent. Dersom transaksjonen er gyldig lagres den i `T_TRANSAKSJON`, mens ugyldige transaksjoner lagres i `T_AVV_TRANSAKSJON` med en avvisningsårsak. Status på valideringen lagres også i `T_INN_TRANSAKSJON`.

3. [WriteToFileService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/WriteToFileService.kt) 
<br/> Tjeneste som genererer en returfil per anvisningsfil som sendes til SPK og som inneholder samme informasjon som anvisningsfilen men med tilleggsinformasjon om status på transaksjonsvalideringen.

4. [SendUtbetalingTransaksjonToOppdragZService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/SendUtbetalingTransaksjonToOppdragZService.kt)
<br/> Tjeneste som sender alle utbetalingstransaksjoner til Oppdragssystemet(OS) som ligger i `T_TRANSAKSJON` (dvs godkjente) men som ennå ikke er oversendt. I tillegg vil også transaksjoner som har feilet ved tidligere oversendelsesforsøk sendes til OS.

5. [SendTrekkTransaksjonToOppdragZService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/SendTrekkTransaksjonToOppdragZService.kt)
<br/> Tjeneste som sender alle trekktransaksjoner til Oppdragssystemet(OS) som ligger i `T_TRANSAKSJON` (dvs godkjente) men som ennå ikke er oversendt. I tillegg vil også transaksjoner som har feilet ved tidligere oversendelsesforsøk sendes til OS.

6. [JmsListenerService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/mq/JmsListenerService.kt) 
<br/> Tjeneste som lytter til reply-meldinger fra OS og lagrer meldingstatus og annen informasjon i T_TRANSAKSJON. Reply-meldingene inneholder informasjon om status til behandlingen av transaksjonene i OS, både utbetalinger- og trekktransaksjoner.
<br/> Tjenesten lytter også til meldinger fra UR Z som inneholder grunnlagsdata som benyttes for å generere avregningsfiler til SPK.
   
7. [AvstemmingService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/AvstemmingService.kt)
<br/> Tjeneste som sender til avstemmingskomponenten all avstemmingsinformasjon om transaksjoner som er sendt til OS siste døgn.


### Kjøring av tjenester i scheduler [JobTaskConfig](../../../src/main/kotlin/no/nav/sokos/spk/mottak/config/JobTaskConfig.kt)
<br/> Tjenestene 1-3 kjører i sekvensiell rekkefølge og startes daglig.
<br/> Tjeneste 4-5 kjøres daglig i sekvensiell rekkefølge og etter at tjeneste 1-3 er ferdig.
<br/> Tjeneste 7 kjøres en gang daglig.

### Ytelse/volum 
<br/> SPK sender månedlig filer på ca 300000 transaksjoner. Daglig sendes maks 2-3 filer med etterutbetalingstransaksjoner med maks noen tusen transaksjoner.
Ved den årlige G-reguleringen sendes en større fil som kan inneholde ca 1 million transaksjoner.
<br/>

### Feilhåndtering
<br/> Feilhåndtering er avhengig av i hvilken tjeneste feilen oppstod. Dersom den oppstod i tjeneste 1-3, vil re-skeduleringen forsøke å rekjøre den feilede tjenesten. Det vil i tillegg bli sendt et varslet om feilen i en slack-kanal. 
<br/> Hver av tjenestene har et startkriterie som sørger for at tjenesten ikke kjører dersom dette ikke er oppfylt. Dersom re-skeduleringen ikke medfører at feilen forsvinner, kreves det en manuell analyse og behandling.