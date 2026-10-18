# Driftsdokumentasjon

Her finner du rutiner for avviste transaksjoner, kjente feil, nye kodeverk og testing før produksjonssetting.

- [Avviste transaksjoner](avviste-transaksjoner.md)
- [Kjente feil](kjente-feil.md)
- [Hvordan legge inn nye kodeverk](nye-kodeverk.md)
- [Test før produksjonssetting](test-for-produksjonssetting.md)

## Unleash-brytere

- `sokos-spk-mottak.schedulerEnabled` slår scheduler-jobbene av eller på.
- `sokos-spk-mottak.mqListenerEnabled` styrer MQ-listenerne. Miljøvariablene `MQ_LISTENER_ENABLED` og
  `AVREGNING_LISTENER_ENABLED` setter fortsatt grenser for hvilke lyttere som kan starte.
- `sokos-spk-mottak.dbHistoryCleanupEnabled` skal styre jobben som sletter gammel historikk.