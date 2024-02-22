````mermaid
    C4Context
        title Avhengighetskart
        System(spk, "SPK", "SPK må sende en fil med transaksjoner")
        System(sokos-spk-mottak, "sokos-spk-mottak", "")
        UpdateElementStyle(sokos-spk-mottak, $fontColor="white", $bgColor="green", $borderColor="black")
    
        Rel(spk, sokos-spk-mottak, "")
    
        System(pesys, "pesys", "Pesys trenger en liste med fødselsnumre som mottar<br />ytelse fra SPK, sender tilbake fullmaktsmottakere")
        BiRel(sokos-spk-mottak, pesys, "")
    
        System(oppdragZ, "Stormaskin Oppdrag Z", "Har behov for å få transaksjoner<br /> og sender tilbake returfil")
    
        BiRel(sokos-spk-mottak, oppdragZ, "")
    
        UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="2")

````
