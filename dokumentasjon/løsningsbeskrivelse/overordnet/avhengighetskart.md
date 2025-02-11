# Avhengighetskart

````mermaid
    C4Context
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="2")
    System(spk, "SPK", "sender anvisningsfil<br>og mottar returfil med status tilbake")
    System(sokos-spk-mottak, "sokos-spk-mottak", "")
    UpdateElementStyle(sokos-spk-mottak, $fontColor="white", $bgColor="green", $borderColor="black")
    System(oppdragZ, "Oppdrag Z", "mottar utbetaling- og trekktransaksjoner og returnerer meldingstatuser")
    System(ur, "UR Z", "sender meldinger med avregningsgrunnlag")
    System(Avstemming, "Avstemmingskomponenten", "mottar avstemmingsdata")
    BiRel(spk, sokos-spk-mottak, "")
    BiRel(sokos-spk-mottak, oppdragZ, "")
    Rel(sokos-spk-mottak, Avstemming, "")
    Rel(ur, sokos-spk-mottak, "")
````
