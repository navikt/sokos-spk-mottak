# ReadAndParseFileService
Tjenesten leser anvisningsfiler fra SPK og validerer at filformatet er riktig. Transaksjonene i filene blir lagret i en innlastingstabell T_INN_TRANSAKSJON.

Filene fra SPK blir lastet ned fra katalogen **/inbound** på en SFTP-server og sorteres etter løpenummer som inngår i filnavnet som har formatet **P611.ANV.NAV.SPK.L<løpenummer>.D<dato>.T<tid>** hvor dato = ddmmyy og tid = hhmmss

**Startbetingelse**: Dersom det finnes filer på SFTP-serveren når tjenesten starter og T_INN_TRANSAKSJON er tom, vil filene prosesseres etter hverandre i rekkefølge angitt av løpenummer i filnavnet.
For hver fil gjøres følgende behandling :

Parser hver record og gjør validering avhengig av recordtype. Hver fil skal ha en startrecord, en eller flere transaksjonrecorder og en sluttrecord. Valideringsreglene for recordtypene er angitt [her](./valideringsregler/filformatvalidering.md)
<br/> Dersom det oppstår en valideringsfeil i en fil, lages det en feilfil med et gitt format som legges på katalogen **/outbound/anvisningsretur** på SFTP-serveren. 
<br/> Dersom det er flere filer som skal behandles etter en fil som feiler, fortsetter valideringen av disse. Dersom feilen ikke er en valideringsfeil derimot, kaster tjenesten en exception og stopper videre behandling.
<br/> Ved valideringsfeil lages en feilfil med en record lik startrecord i anvisningsfilen hvor statuskode og feiltekst inneholder informasjon om valideringsfeilen. Filnavnet har formatet **SPK_NAV_<tidspunkt>_INL** hvor tidspunkt = yyyyMMdd_HHmmss. 

Når filbehandlingen er over (uavhengig av om det oppstod en valideringsfeil), blir følgende utført:
* T_LOPENR blir oppdatert med løpenummeret til anvisningsfilen dersom filen ikke inneholder løpenummerfeil, uriktig anviser (dvs ikke SPK) eller ugyldig filtype (dvs ikke ANV). 
Denne tabellen angir siste anvendte filløpenummer i løpenummersekvensen og kan følgelig ikke gjenbrukes. 
Hvis løpenummer-oppdateringen ikke kan utføres, betyr det at anvisningsfilen kan gjenbruke samme løpenummer dersom filinnholdet korrigeres og filen sendes på nytt.
* T_FIL_INFO blir oppdatert med informasjon om anvisningsfilen, slik som valideringstatus, anvisertype SPK, filtype ANV (anvisningsfil), filtilstand-status GOD (godkjent)  eller AVV (avvist), filnavn, løpenummer og feiltekst ved valideringsfeil.
* Anvisningsfilen flyttes til katalogen **/inbound/ferdig** på SFTP-serveren.

Dersom filparsingen ikke feiler, vil T_INN_TRANSAKSJON blir lastet med alle transaksjonene i filen.

Når alle transaksjonene er lest inn, blir anvisningsfilen flyttet til katalogen **/inbound/ferdig** på SFTP-serveren. 

Filformat starterecord:
Filformat transaksjonrecord:
Filformat sluttrecord:
Mapping T_INN_TRANSAKSJON:
Mapping T_FIL_INFO:
