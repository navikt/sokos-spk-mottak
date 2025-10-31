# Produksjonsetting

Før nye ting settes i produksjon, er det viktig å teste at alt fungerer som forventet i Q1. 

Her er en generell fremgangsmåte for hvordan du kan gjøre dette:

1. Koble seg til SFTP-serveren i Q1 ved hjelp av en SFTP-klient.
   1. IP-adressen finner du her https://confluence.adeo.no/spaces/linuxdrift/pages/70202292/Ekstern+filsluse
   2. Brukernavn finner du her https://confluence.adeo.no/spaces/linuxdrift/pages/315968124/Oversikt+over+samhandlere+sftp.nav.no
   3. Private SSH-nøkkel finner du under `privateKey` og passordet til ssh-nøkkelen finner du under `keyPassword`. Disse verdiene finner du på [vault.adeo.no](https://vault.adeo.no) secrets i path under `kv/preprod/fss` i mappen `sokos-spk-mottak`.

2. Last opp testfiler til den `inbound/anvinsingsfil` mappen på SFTP-serveren.
3. Kjør følgende SQL-kommando før testing:
    ```
   ALTER TABLE MO611Q1.T_INN_TRANSAKSJON
    ALTER COLUMN INN_TRANSAKSJON_ID
        RESTART WITH 87656231;
   ```
`RESTART WITH` ska være `RESTART WITH XX` hvor XX = max(INN_TRANSAKSJON_ID) + 1 til tabellen `T_INN_TRANSAKSJON`. 

4. Verifiser i logger og databasen om filen er prosessert uten feil.