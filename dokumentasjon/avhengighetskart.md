
# Avhengighetskart
````mermaid
    C4Context
        UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="2")

        System(spk, "SPK", "SPK må sende en fil med transaksjoner")
        System(sokos-spk-mottak, "sokos-spk-mottak", "")
        UpdateElementStyle(sokos-spk-mottak, $fontColor="white", $bgColor="green", $borderColor="black")
        System(pesys, "pesys", "Pesys trenger en liste med fødselsnumre som mottar<br />ytelse fra SPK, sender tilbake fullmaktsmottakere")
        System(oppdragZ, "Stormaskin Oppdrag Z", "Har behov for å få transaksjoner<br /> og sender tilbake returfil")
    
        Rel(spk, sokos-spk-mottak, "")
        BiRel(sokos-spk-mottak, pesys, "")
        BiRel(sokos-spk-mottak, oppdragZ, "")
````
