# Avhengighetskart

````mermaid
    C4Context
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="2")

    System(spk, "SPK", "SPK sender transaksjonsfil<br>og mottar transaksjonsfil med status tilbake")
    System(sokos-spk-mottak, "sokos-spk-mottak", "")
    UpdateElementStyle(sokos-spk-mottak, $fontColor="white", $bgColor="green", $borderColor="black")
    System(oppdragZ, "Oppdrag Z", "Får utbetaling- og trekk transaksjoner")

    BiRel(spk, sokos-spk-mottak, "")
    Rel(sokos-spk-mottak, oppdragZ, "")
````
