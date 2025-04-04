# Overordnet beskrivelse

NAV gjennomfører utbetaling av ytelser og trekk på vegne av SPK og mottar daglig filer med transaksjoner som skal behandles i økonomiløsningen. Transaksjonene blir validert og lastet inn i den nye mottakskomponenten `sokos-spk-mottak` og deretter overført til Oppdragssystemet (OS) for videre behandling i økonomiløsningen. Videre mottar `sokos-spk-mottak` daglig meldinger med grunnlagsdata som benyttes for å utføre avregninger som sendes som filer til SPK.
`sokos-spk-mottak` består av følgende tjenester:

1. [ReadFileService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/ReadFileService.kt)
<br/> Tjeneste som leser inn ubehandled(e) fil(er) med transaksjoner fra SPK og validerer formatet på filen. Dersom filen aksepteres, lagres innholdet i en midlertidig innlastingstabell, `T_INN_TRANSAKSJON`. Dersom den ikke aksepteres, lagres ikke innholdet og det produseres en returfil som beskriver feilen og som sendes tilbake til SPK.
   
2. [ValidateTransaksjonService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/ValidateTransaksjonService.kt) 
<br/> Tjeneste som behandler transaksjonene som er lastet inn i innlastingstabellen i forrige trinn. Dette består av validering av transaksjonene som ligger i tabellen og lagre disse permanent. Dersom transaksjonen er gyldig lagres den i `T_TRANSAKSJON`, mens ugyldige transaksjoner lagres i `T_AVV_TRANSAKSJON` med en avvisningsårsak. Status på valideringen lagres også i `T_INN_TRANSAKSJON`.

3. [SendInnlesningsreturService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/SendInnlesningsreturService.kt) 
<br/> Tjeneste som genererer en returfil per anvisningsfil som sendes til SPK og som inneholder samme informasjon som anvisningsfilen men med tilleggsinformasjon om status på transaksjonsvalideringen.

4. [SendUtbetalingService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/SendUtbetalingService.kt)
<br/> Tjeneste som sender alle utbetalingstransaksjoner til Oppdragssystemet(OS) som ligger i `T_TRANSAKSJON` (dvs godkjente) men som ennå ikke er oversendt. I tillegg vil også transaksjoner som har feilet ved tidligere oversendelsesforsøk sendes til OS.

5. [SendTrekkService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/SendTrekkService.kt)
<br/> Tjeneste som sender alle trekktransaksjoner til Oppdragssystemet(OS) som ligger i `T_TRANSAKSJON` (dvs godkjente) men som ennå ikke er oversendt. I tillegg vil også transaksjoner som har feilet ved tidligere oversendelsesforsøk sendes til OS.

6. [UtbetalingListenerService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/mq/UtbetalingListenerService.kt) 
<br/> Tjeneste som lytter til Utbetaling transaksjon reply-meldinger fra OS og lagrer meldingstatus og annen informasjon i T_TRANSAKSJON. Reply-meldingene inneholder informasjon om status til behandlingen av transaksjonene i OS.

7. [TrekkListenerService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/mq/TrekkListenerService.kt)
<br/> Tjeneste som lytter til Trekk transaksjon reply-meldinger fra OS og lagrer meldingstatus og annen informasjon i T_TRANSAKSJON. Reply-meldingene inneholder informasjon om status til behandlingen av transaksjonene i OS.

8. [AvstemmingService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/AvstemmingService.kt)
<br/> Tjeneste som sender til avstemmingskomponenten all avstemmingsinformasjon om transaksjoner som er sendt til OS siste døgn.

9. [AvregningListenerService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/mq/AvregningListenerService.kt)
<br/> Tjeneste som lytter til meldinger fra UR Z som inneholder grunnlagsdata som benyttes for å generere avregningsfiler til SPK.

10. [SendAvregningsreturService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/SendAvregningsreturService.kt)
<br/> Tjeneste som genererer en returfil for avregning som ikke har sendt som til SPK før. Returfilen skal innhole avregningsdata og har samme mal som *innlesningsretur*.    

11. [LeveattestService](../../../src/main/kotlin/no/nav/sokos/spk/mottak/service/LeveAttestService.kt)
<br/> Tjeneste som leverer leveattester gjennom REST-grensesnitt til Pensjon-PEN. Tjenesten henter leveattester fra `T_TRANSAKSJON` for anviser `SPK` for en gitt periode.

### Kjøring av tjenester i scheduler [JobTaskConfig](../../../src/main/kotlin/no/nav/sokos/spk/mottak/config/JobTaskConfig.kt)
<br/> Tjenestene 1-3 kjører i sekvensiell rekkefølge og startes daglig.
<br/> Tjeneste 4-5 kjøres daglig i sekvensiell rekkefølge og etter at tjeneste 1-3 er ferdig.
<br/> Tjeneste 7 kjøres en gang daglig.
<br/> Tjeneste 9 kjøres en gang daglig.

### Ytelse/volum 
<br/> SPK sender månedlig filer på ca 300000 transaksjoner. Daglig sendes maks 2-3 filer med etterutbetalingstransaksjoner med maks noen tusen transaksjoner.
Ved den årlige G-reguleringen sendes en større fil som kan inneholde ca 1 million transaksjoner.
<br/>

### Feilhåndtering
<br/> Feilhåndtering er avhengig av i hvilken tjeneste feilen oppstod. Dersom den oppstod i tjeneste 1-3, vil re-skeduleringen forsøke å rekjøre den feilede tjenesten. Det vil i tillegg bli sendt et varslet om feilen i en slack-kanal. 
<br/> Hver av tjenestene har et startkriterie som sørger for at tjenesten ikke kjører dersom dette ikke er oppfylt. Dersom re-skeduleringen ikke medfører at feilen forsvinner, kreves det en manuell analyse og behandling.