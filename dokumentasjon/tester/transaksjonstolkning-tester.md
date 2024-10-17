# Tester for transaksjonstolkning og fnrEndret
## Tester hvor personen eksisterer

 - t_inn_transaksjon har beløpstype Utbetaling(01) og det finnes kun en transaksjon med beløpstype Trekk(03) --> transTolkning = NY
 - t_inn_transaksjon har beløpstype Utbetaling(01) og det finnes en transaksjon med samme beløpstype Utbetaling(02) --> transTolkning = NY_EKSIST
 - t_inn_transaksjon har beløpstype Utbetaling(01) og art(ETT) og det finnes en transaksjon med samme beløpstype Utbetaling(01) og art(BPE) i samme fagområde men med et annet fnr --> transTolkning = NY_EKSIST og fnrEndret satt
 - t_inn_transaksjon har beløpstype Utbetaling(01) og det finnes en transaksjon med samme beløpstype Utbetaling(01) men med annen anviser --> transTolkning = NY
 - t_inn_transaksjon har beløpstype Utbetaling(01) og art(ETT) og det finnes en transaksjon med samme beløpstype Utbetaling(02) men med annen art(BPE) i samme fagområde --> transTolkning = NY_EKSIST
 - t_inn_transaksjon har beløpstype Utbetaling(01) og art(ETT) og det finnes en transaksjon med samme beløpstype Utbetaling(01) men med annen art(UFT) i et annet fagområde og annen anviser --> transTolkning = NY
 - t_inn_transaksjon har beløpstype Utbetaling(01) og art(ETT) og det finnes en transaksjon med samme beløpstype Utbetaling(02) men med annen art(UFT) i et annet fagområde og  en transaksjon med samme beløpstype Utbetaling(01) men med annen art(RNT) i samme fagområde --> transTolkning = NY_EKSIST
 - t_inn_transaksjon har 2 transaksjoner med samme fnr, beløpstype Utbetaling(01) og ulike art(ETT og UFT) som tilhører ulike fagområder og det finnes en transaksjon med samme beløpstype Utbetaling(02) men med annen art(UFE) i samme  fagområde som en av inn-transaksjonene --> transTolkning = NY(for ETT-transaksjon) og NY_EKSIST(for UFT-transaksjon)
 - t_inn_transaksjon har 4 transaksjoner med ulike fnr, beløpstype Utbetaling(01) og art(ETT) og det finnes transaksjoner med samme beløpstype Utbetaling(01,02) og en med annen art(UFE,UFT) i annet fagområde, en  med annen art(RNT,ALD) i samme fagområde og en har en eksisterende art(ETT) og siste inn-transaksjon har ingen tidligere art for person --> transTolkning = NY(for UFE/UFT-transaksjon og ny person) og NY_EKSIST(for RNT/ALD/ETT-transaksjoner)
 - t_inn_transaksjon har 3 transaksjoner med ulike fnr, beløpstype Utbetaling(01) og art(ETT) og det finnes transaksjoner med samme beløpstype Utbetaling(01,02) og en med annen art(UFE,UFT) i annet fagområde og endret fnr, en med annen art(RNT,ALD) i samme fagområde og endret fnr og en har en eksisterende art(ETT) og endret fnr --> transTolkning = NY(for UFE/UFT-transaksjon) og NY_EKSIST(for RNT/ALD/ETT-transaksjoner) og fnrEndret satt for alle transaksjoner

## Tester hvor personen ikke eksisterer
- t_inn_transaksjon har beløpstype Utbetaling(01) og det finnes ingen transaksjon for denne personen --> transTolkning = NY
- t_inn_transaksjon har 2 transaksjoner med samme fnr, beløpstype Utbetaling(01,02) og art tilhørende ulike fagområder (ETT og UFT) og og det finnes ingen transaksjoner for denne personen --> transTolkning = NY for begge transaksjoner
- t_inn_transaksjon har 2 transaksjoner med samme fnr, beløpstype Utbetaling(01,02) og art tilhørende samme fagområder (ETT og ALD) og det finnes ingen transaksjoner for denne personen --> transTolkning = NY for første transaksjon(ETT) og NY_EKSIST for andre(ALD)
- t_inn_transaksjon har 4 transaksjoner med samme fnr, beløpstype Utbetaling(01,02) og 2 og 2 har art tilhørende samme fagområder (ETT og ALD) og (UFT og UFE) og det finnes ingen transaksjoner for denne personen --> transTolkning = NY for første transaksjon med art som representerer et nytt fagområde(ETT og UFE) og NY_EKSIST for de andre(ALD og UFT)
