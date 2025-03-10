# Avhengighetskart

````mermaid
    C4Context
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="2")
    System(pensjon-pen, "pensjon-pen", "henter leveattester via <br>REST-grensesnitt på JSON-format")
    System(sokos-spk-mottak, "sokos-spk-mottak")
    System(spk, "SPK", "sender anvisningsfil<br>og mottar returfil med status tilbake")
    UpdateElementStyle(sokos-spk-mottak, $fontColor="white", $bgColor="green", $borderColor="black")
    System(oppdragZ, "Oppdrag Z", "mottar utbetaling- og trekktransaksjoner og returnerer meldingstatuser")
    System(ur, "UR Z", "sender meldinger med avregningsdata")
    System(Avstemming, "Avstemmingskomponenten", "mottar avstemmingsdata")
    BiRel(spk, sokos-spk-mottak, "")
    BiRel(sokos-spk-mottak, oppdragZ, "")
    Rel(sokos-spk-mottak, Avstemming, "")
    Rel(ur, sokos-spk-mottak, "")
    BiRel(sokos-spk-mottak, pensjon-pen, "")
````
