# Avhengighetskart

````mermaid
    C4Context
    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="2")

    System(spk, "SPK", "SPK sender transaksjons-fil<br>og mottar transaksjons-status tilbake")
    System(sokos-spk-mottak, "sokos-spk-mottak", "")
    UpdateElementStyle(sokos-spk-mottak, $fontColor="white", $bgColor="green", $borderColor="black")
    System(pesys, "pesys", "Pesys får liste med fødselsnumre som mottar<br />ytelse fra SPK, sender tilbake fullmaktsmottakere")
    System(oppdragZ, "Oppdrag Z", "Får utbetaling- og trekk transaksjoner")

    BiRel(spk, sokos-spk-mottak, "")
    BiRel(sokos-spk-mottak, pesys, "")
    Rel(sokos-spk-mottak, oppdragZ, "")
````
