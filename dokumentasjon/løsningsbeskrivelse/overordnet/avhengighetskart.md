# Avhengighetskart

````mermaid
    C4Context
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="2")
    System(spk, "SPK", "sender anvisningsfil<br>og mottar returfil med status tilbake")
    System(sokos-spk-mottak, "sokos-spk-mottak", "")
    UpdateElementStyle(sokos-spk-mottak, $fontColor="white", $bgColor="green", $borderColor="black")
    System(oppdragZ, "OppdragZ", "mottar utbetaling- og trekktransaksjoner og returnerer meldingstatuser")
    System(Avstemming, "Avstemmingskomponenten", "mottar avstemmingsdata")
    BiRel(spk, sokos-spk-mottak, "")
    BiRel(sokos-spk-mottak, oppdragZ, "")
    Rel(sokos-spk-mottak, Avstemming, "")
````
