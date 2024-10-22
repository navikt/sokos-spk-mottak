# Tester for validering av filformat på anvisningsfiler fra SPK

| Beskrivelse av test                                                                                           | Feilstatus |
|---------------------------------------------------------------------------------------------------------------|------------| 
| startrecord har anviser = SPX                                                                                 | 01         |
| startrecord har mottaker = NAX                                                                                | 02         |
| startrecord har filløpenummer som er tidligere mottatt                                                        | 03         |
| startrecord har filløpenummer = 4X                                                                            | 04         |
| startrecord har filløpenummer som er større enn siste løpenummer + 1                                          | 04         |
| startrecord har filtype = ANX                                                                                 | 05         |
| startrecord har recordtype = 11                                                                               | 06         |
| sluttrecord har recordtype = 10                                                                               | 06         |
| transaksjonsrecord har recordtype = 03                                                                        | 06         |
| filen har 1 transaksjonsrecord, men sluttrecorden angir antall = 8                                            | 07         |
| sluttrecorden angir beløpssum = 2775100 som ikke er lik summen av beløpene til transaksjonsrecordene = 346900 | 08         |
| transaksjonsrecord har beløp = 346X00                                                                         | 08         |
| startrecord har produksjonsdato = 20240199                                                                    | 09         |

