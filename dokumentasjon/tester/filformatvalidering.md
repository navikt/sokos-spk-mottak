# Tester for validering av filformat på anvisningsfiler fra SPK

 - startrecord har anviser = SPX --> feilstatus 01 (ugyldig anviser)
 - startrecord har mottaker = NAX --> feilstatus 02 (ugyldig mottaker)
 - startrecord har filløpenummer som er tidligere mottatt --> feilstatus 03 (filløpenummer i bruk)
 - startrecord har filløpenummer = 4X  --> feilstatus 04 (ugyldig filløpenummer)
 - startrecord har filløpenummer som er større enn siste løpenummer + 1 --> feilstatus 04 (ugyldig filløpenummer)
 - startrecord har filtype = ANX --> feilstatus 05 (ugyldig filtype)
 - startrecord har recordtype = 11 --> feilstatus 06 (ugyldig recordtype)
 - sluttrecord har recordtype = 10 --> feilstatus 06 (ugyldig recordtype)
 - transaksjonsrecord har recordtype = 03 --> feilstatus 06 (ugyldig recordtype)
 - filen har 1 transaksjonsrecord, men sluttrecorden angir antall = 8  --> feilstatus 07 (ugyldig antall records)
 - sluttrecorden angir beløpssum(2775100) som ikke er lik summen av beløpene til transaksjonsrecordene(346900)   --> feilstatus 08 (ugyldig sumbeløp)
 - transaksjonsrecord har beløp = 346X00 --> feilstatus 08 (ugyldig sumbeløp)
 - startrecord har produksjonsdato = 20240199 --> feilstatus 09 (ugyldig produksjonsdato)
