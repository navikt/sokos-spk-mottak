# Avviste transaksjoner

## Avviste transaksjoner

Når du får et Slack-varsel om at avvikstransaksjoner er opprettet, har valideringen lagret avviste transaksjoner i DB2-tabellen `T_AVV_TRANSAKSJON`. Varslet oppgir hvor mange transaksjoner som ble avvist.

Når du får varslet på vakt, undersøk avvikene som ble opprettet rundt tidspunktet for varslet. Denne spørringen viser avvik fra det siste døgnet uten å hente personidentifikatorer:

```sql
SELECT AVV_TRANSAKSJON_ID,
       FIL_INFO_ID,
       K_TRANSAKSJON_S,
       BELOPSTYPE,
       ART,
       DATO_OPPRETTET
FROM T_AVV_TRANSAKSJON
WHERE DATO_OPPRETTET > CURRENT_TIMESTAMP - 1 DAYS
ORDER BY DATO_OPPRETTET DESC;
```

Bruk statuskoden og opplysningene om transaksjonen til å finne årsaken. Se [valideringsreglene for transaksjoner](../løsningsbeskrivelse/detaljert/valideringsregler/transaksjonsvalidering.md) for mulige avvisningsårsaker.
