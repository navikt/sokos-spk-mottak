# Innholdsoversikt

## Overordnet beskrivelse
- [Overordnet](løsningsbeskrivelse/overordnet/overordnet.md)
- [Domenemodell](løsningsbeskrivelse/overordnet/domenemodell.md)
- [Avhengighetskart](løsningsbeskrivelse/overordnet/avhengighetskart.md)
- [Flowchart](løsningsbeskrivelse/overordnet/flowchart.md)

## Detaljert beskrivelse av tjenestene
- [ReadAndParseFileService](løsningsbeskrivelse/detaljert/ReadAndParseFileService.md)
- [ValidateTransaksjonService](løsningsbeskrivelse/detaljert/ValidateTransaksjonService.md)
- [WriteToFileService](løsningsbeskrivelse/detaljert/WriteToFileService.md)
- [SendUtbetalingTransaksjonToOppdragZService](løsningsbeskrivelse/detaljert/SendUtbetalingTransaksjonToOppdragZService.md)
- [SendTrekkTransaksjonToOppdragZService](løsningsbeskrivelse/detaljert/SendTrekkTransaksjonToOppdragZService.md)
- [JmsListenerService](løsningsbeskrivelse/detaljert/JmsListenerService.md)
- [AvstemmingService](løsningsbeskrivelse/detaljert/AvstemmingService.md)


## Tester
- [Filformat tester](tester/filformat-tester.md)
- [Transaksjonstolkning tester](tester/transaksjonstolkning-tester.md)
- [Transaksjonvalidering tester](tester/transaksjonsvalidering-tester.md)

## Kladd for resterende utfasing av MOT 5,6,7,10
- Standup hver mandag
- Diskutere løsningsforslag og milepæler for batch 5,6,7,10
- Lage milepæler - Hva milepælen inneholder, definerer oppgaver, hva som skal gjøres
- Dokumentasjon - Dokumenterer underveis, dokumentasjon av tester, funksjonalitet osv
- Testing - Vi gjør følgende; Når en milepæl er ferdig, booker vi en halv dags møte hvor vi går gjennom hele servicen/tjenesten samt funksjonalitet, tester, hva som er gjort osv. Og at vi blir enig om at vi er fornøyd før vi går videre,

- Hva inneholder batch 5,6,7,10 per dags dato:
  - 5: Innlesning av avregningsgrunnlag fil som kommer fra både OS or UR som er samme filformat som anvsiningsfilen. Leser inn filen, validerer records, laster innholdet inn i Innlastingstabell (T_INN_RETUR) for avregning og avstemming. Deretter fjernes filene som er ferdig behandlet. Hvis feil med filen så genereres det feilfil med en record som beskriver hva som er feil med filen. (ref systemdokumentasjon https://confluence.adeo.no/display/OKSY/MOT+-+Systemdokumentasjon og https://github.com/navikt/sokos-mot/blob/master/Dokumentasjon/5-6-7-8-avregningsretur-avstemming.md)
  - 6: Behandle avregningsretur fra data som er lagret i innlastingstabellen (T_INN_RETUR) i BATCH 5 og kobler med T_TRANSAKSJON tabellen for å finne ut hvilken transaksjoner som er avregnet og som SPK skal få informasjon om. Oppdaterer også tabellen (T_RETUR_TIL_ANV) med kobling til informasjon til (T_TRANSAKSJON)
  - 7: Genererer fil og sender filen tilbake til SPK og oppdaterer T_FIL_INFO tabellen med informasjon om filen som er sendt til SPK
  - 10: Leveattester - Pensjon trenger informasjon om leveattester for å sjekke om det er personer som får utbetalinger via MOT som ikke har utbetalinger  i pensjon systemet. 

- Løsningsforslag:
  - **Avregningsservice**
    - https://github.com/navikt/sokos-spk-mottak/blob/feature/motta-avregningsgrunnlag/dokumentasjon/l%C3%B8sningsbeskrivelse/detaljert/JmsListenerService.md
    - Hente trekk - Ikke avklart (Må diskutere litt nærmere hva vi gjør her)
    - Per idag får vi flere transaksjoner på fagområder enn vi har behov for fra UR. Snakke med UR om de kan begrense utrekket til SPK-transaksjoner som kommer fra MOT
      - Hvis UR ikke har kapasitet for endringer så må vi ta en diskusjon om hvordan vi løser dette.
  - **SendavregningstransakjsonerService**
      - Leser fra T_RETUR_TIL_ANV med gitt tidspunkt og genererer fil til SPK
      - Oppdater T_RETUR_TIL_ANV med informasjon om filen som er sendt til SPK
  - **LeveattestService**
    - Lage en REST-grensesnitt i sokos-spk-mottak som Pensjon kan kalle for å hente ut informasjon om leveattester
      - Levere ut en JSON som inneholder liste av fnr som ikke har utbetalinger i pensjonssystemet

- Milepæl 1: Avregningsservice 
- Milepæl 2: SendavregningstransakjsonerService
- Milepæl 3: LeveattestService

