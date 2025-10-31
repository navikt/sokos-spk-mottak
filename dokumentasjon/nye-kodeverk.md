# Hvordan legge inn nye kodeverk

Vi trenger å legge til nye kodeverk i databasen når SPK sender oss nye koder som skal brukes i applikasjonen.

Arttype (K_ART) og klassifikasjon (OS_KLASSIFIKASJON) skal vi få fra den som bestiller endringen i SPK.

Nedenfor er et eksempel:

#### T_K_ART

```sql
    INSERT INTO T_K_ART (K_ART, DEKODE, DATO_FOM, DATO_TOM, ER_GYLDIG, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV)
    VALUES ('TPE', 'Tidligpensjon', DATE ('2025-11-01'), NULL, '1', CURRENT_TIMESTAMP, 'TOB-5450', CURRENT_TIMESTAMP, 'TOB-5450');
```

#### T_K_GYLDIG_KOMBIN

```sql
    INSERT INTO T_K_GYLDIG_KOMBIN (K_GYLDIG_KOMBIN_ID, K_ART, K_BELOP_T, K_TREKKGRUPPE, K_TREKK_T, K_TREKKALT_T, K_ANVISER, K_FAGOMRADE, OS_KLASSIFIKASJON, DATO_FOM, DATO_TOM, ER_GYLDIG,
                                   DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV)
    VALUES (91, 'TPE', '01', NULL, NULL, NULL, 'SPK', 'PENSPK', 'PENSPKTIDLPENSJ', DATE ('2025-11-01'), NULL, '1', CURRENT_TIMESTAMP, 'TOB-5450', CURRENT_TIMESTAMP, 'TOB-5450');
```

Oppdatering i kode. [OppdragConverter.kt](../src/main/kotlin/no/nav/sokos/spk/mottak/domain/converter/OppdragConverter.kt):

Legg til nye kodeverk som skal mappes til OppdragZ i `gradTypeMap`.

```kotlin
 private val gradTypeMap =
    mapOf(
        "AFP" to "AFPG",
        "ALD" to "UTAP",
        "BPE" to "UBGR",
        "ETT" to "UBGR",
        "RNT" to "UBGR",
        "UFO" to "UFOR",
        "UFT" to "UFOR",
        "U67" to "UFOR",
        "UFE" to "UFOR",
        "ALP" to "UTAP",
        "PSL" to "UTAP",
        "AFL" to "AFPG",
        "BTP" to "UBGR",
        "OVT" to "UBGR",
    )
```

NB! Husk å oppdatere [db2Script.sql](../src/test/resources/database/db2Script.sql) med nye insert-setninger for kodeverkene som legges til i databasen.