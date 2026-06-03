# LeveAttestService
Tjenesten leverer leveattester gjennom REST-grensesnitt til Pensjon-PEN. Tjenesten henter leveattester fra `T_TRANSAKSJON` for anviser `SPK` for en gitt periode.

Parameter som sendes til tjenesten:
* ```{datoFom}``` - Dato fra som angir startdato for perioden som skal hentes leveattester for. Datoen må være på formatet yyyy-MM-dd.
Når anvisningsfilen er lest inn og valideringen er gjennomført, vil det legges inn en `DATO_FOM` i `T_TRANSAKSJON` som angir perioden som utbetaling gjelder for.
Responsen som returneres fra tjenesten er en liste av av objekter som har `fnrFk` og `kAnviser`.
Følgende respons ser slik ut:
```
[
    {
        "fnrFk": "12345678901",
        "kAnviser": "123456"
    },
    {
        "fnrFk": "12345678902",
        "kAnviser": "123457"
    }
]
```