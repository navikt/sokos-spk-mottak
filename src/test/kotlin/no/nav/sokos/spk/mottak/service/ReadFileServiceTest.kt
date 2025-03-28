package no.nav.sokos.spk.mottak.service

import java.io.IOException
import java.time.LocalDate

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.common.KotestInternal
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk

import no.nav.sokos.spk.mottak.SPK_FEIL_FILLOPENUMMER_I_BRUK
import no.nav.sokos.spk.mottak.SPK_FEIL_FORVENTET_FILLOPENUMMER
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_ANTRECORDS
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_ANVISER
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_END_RECTYPE
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_FILTYPE
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_LOPENUMMER
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_MOTTAKER
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_PRODDATO
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_START_RECTYPE
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_SUMBELOP
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE
import no.nav.sokos.spk.mottak.SPK_FILE_FEIL
import no.nav.sokos.spk.mottak.SPK_OK
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.TestHelper.verifyFilInfo
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.domain.BEHANDLET_NEI
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_AVV
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.FILTYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.LopeNummer
import no.nav.sokos.spk.mottak.domain.READ_FILE_SERVICE
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.Db2Listener.dataSource
import no.nav.sokos.spk.mottak.listener.SftpListener

private const val MAX_LOPENUMMER = 33

@OptIn(KotestInternal::class)
internal class ReadFileServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, SftpListener))

        val ftpService: FtpService by lazy {
            FtpService(SftpConfig(SftpListener.sftpProperties))
        }

        val readFileService: ReadFileService by lazy {
            ReadFileService(
                dataSource = dataSource,
                ftpService = ftpService,
            )
        }

        afterEach {
            SftpListener.deleteFile(
                Directories.INBOUND.value + "/P611*",
                Directories.ANVISNINGSFIL_BEHANDLET.value + "/P611*",
                Directories.ANVISNINGSRETUR.value + "/SPK_NAV_*",
            )
        }

        Given("det finnes en ubehandlet fil i \"inbound\" på FTP-serveren ") {
            ftpService.createFile(SPK_OK, Directories.INBOUND, readFromResource("/spk/$SPK_OK"))

            When("leser filen og parser") {
                readFileService.readAndParseFile()

                Then("skal filen bli flyttet fra \"inbound\" til \"inbound/anvisningsfilbehandlet\" og transaksjoner blir lagret i databasen.") {
                    ftpService.downloadFiles(Directories.ANVISNINGSFIL_BEHANDLET).size shouldBe 1

                    val lopeNummerFraFil = "000034"
                    val lopeNummer = Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil)
                    verifyLopenummer(lopeNummer)

                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_GOD, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.OK, FILTILSTANDTYPE_GOD)

                    val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfoList.first().filInfoId!!)
                    inntransaksjonList.size shouldBe 8
                    verifyInntransaksjon(inntransaksjonList.first(), filInfoList.first().filInfoId!!)
                }
            }
        }

        Given("det finnes to ubehandlede filer i \"inbound\" på FTP-serveren ") {
            ftpService.createFile(SPK_OK, Directories.INBOUND, readFromResource("/spk/$SPK_OK"))
            ftpService.createFile(SPK_FILE_FEIL, Directories.INBOUND, readFromResource("/spk/$SPK_FILE_FEIL"))

            When("leser begge filene og parser") {
                readFileService.readAndParseFile()

                Then("skal begge filene bli flyttet fra \"inbound\" til \"inbound/ferdig\", transaksjoner blir lagret i databasen og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                    ftpService.downloadFiles(Directories.ANVISNINGSFIL_BEHANDLET).size shouldBe 2
                    val anvisningFiler = ftpService.downloadFiles(Directories.ANVISNINGSRETUR)
                    anvisningFiler.size shouldBe 1
                    anvisningFiler.forEach { (_, value) ->
                        value shouldContain "01SPK        NAV        000035ANV20240131ANVISNINGSFIL                      08Feil sumbeløp                      "
                    }

                    val lopeNummerFraFil = "000035"
                    val lopenummer = Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil)
                    verifyLopenummer(lopenummer)

                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    val feiltekst = "Total beløp 2775100 stemmer ikke med summeringen av enkelt beløpene 2775200"
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, feiltekst)

                    val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfoList.first().filInfoId!!)
                    inntransaksjonList.shouldBeEmpty()
                }
            }
        }

        @Suppress("NAME_SHADOWING")
        Given("det finnes ubehandlet fil i \"inbound\" på FTP-serveren ") {
            val ftpServiceMock = mockk<FtpService>()
            val readFileService: ReadFileService by lazy {
                ReadFileService(
                    dataSource = dataSource,
                    ftpService = ftpServiceMock,
                )
            }

            When("leser ok format filen og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_OK to readFromResource("/spk/$SPK_OK").lines())

                Then("skal det kastet en MottakException med uforventet feil") {
                    every { ftpServiceMock.moveFile(any(), any(), any()) } throws IOException("Ftp server is down!")
                    val exception =
                        shouldThrow<MottakException> {
                            readFileService.readAndParseFile()
                        }
                    exception.message shouldBe "Ukjent feil ved innlesing av fil: P611.ANV.NAV.HUB.SPK.L000034.D240104.T003017_OK.txt. Feilmelding: Ftp server is down!"
                }
            }

            When("leser feil format filen og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FILE_FEIL to readFromResource("/spk/$SPK_FILE_FEIL").lines())

                Then("skal det kastet en MottakException med uforventet feil") {
                    every { ftpServiceMock.createFile(any(), any(), any()) } throws IOException("Ftp server can not move file!")
                    val exception =
                        shouldThrow<MottakException> {
                            readFileService.readAndParseFile()
                        }
                    exception.message shouldBe "Feil ved opprettelse av avviksfil: P611.ANV.NAV.HUB.SPK.L000035.D240104.T003017_FEIL.txt. Feilmelding: Ftp server can not move file!"
                }
            }

            When("leser fil med ugyldig anviser og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_ANVISER to readFromResource("/spk/$SPK_FEIL_UGYLDIG_ANVISER").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_ANVISER") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_ANVISER, FILTILSTANDTYPE_AVV, "Ugyldig anviser")
                    Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
                }
            }

            When("leser fil med ugyldig mottaker og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_MOTTAKER to readFromResource("/spk/$SPK_FEIL_UGYLDIG_MOTTAKER").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_MOTTAKER") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_MOTTAKER, FILTILSTANDTYPE_AVV, "Ugyldig mottaker")
                    verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
                }
            }

            When("leser fil med løpenummer som er i bruk og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_FILLOPENUMMER_I_BRUK to readFromResource("/spk/$SPK_FEIL_FILLOPENUMMER_I_BRUK").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus FILLOPENUMMER_I_BRUK") {
                    val lopeNummerFraFil = "000032"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.FILLOPENUMMER_I_BRUK, FILTILSTANDTYPE_AVV, "Filløpenummer $lopeNummerFraFil allerede i bruk")
                    Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
                }
            }

            When("leser fil med ugyldig løpenummer og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_LOPENUMMER to readFromResource("/spk/$SPK_FEIL_UGYLDIG_LOPENUMMER").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_FILLOPENUMMER") {
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf("00004X"))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_FILLOPENUMMER, FILTILSTANDTYPE_AVV, "Filløpenummer format er ikke gyldig")
                    Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
                }
            }

            When("leser fil med ikke forventet løpenummer og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_FORVENTET_FILLOPENUMMER to readFromResource("/spk/$SPK_FEIL_FORVENTET_FILLOPENUMMER").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus FORVENTET_FILLOPENUMMER") {
                    val lopeNummerFraFil = "000099"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.FORVENTET_FILLOPENUMMER, FILTILSTANDTYPE_AVV, "Forventet lopenummer 34")
                    Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
                }
            }

            When("leser fil med ugyldig filtype og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_FILTYPE to readFromResource("/spk/$SPK_FEIL_UGYLDIG_FILTYPE").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_FILTYPE") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_FILTYPE, FILTILSTANDTYPE_AVV, "Ugyldig filtype", "ANX", "SPK")
                    Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
                }
            }

            When("leser fil med ugyldig antall record og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_ANTRECORDS to readFromResource("/spk/$SPK_FEIL_UGYLDIG_ANTRECORDS").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_ANTRECORDS") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(
                        filInfoList.first(),
                        FilStatus.UGYLDIG_ANTRECORDS,
                        FILTILSTANDTYPE_AVV,
                        "Oppsummert antall records oppgitt i sluttrecord er 8 og stemmer ikke med det faktiske antallet 3",
                    )
                    verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
                }
            }

            When("leser fil med ugyldig sumbeløp og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_SUMBELOP to readFromResource("/spk/$SPK_FEIL_UGYLDIG_SUMBELOP").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_SUMBELOP") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, "Total beløp 2775100 stemmer ikke med summeringen av enkelt beløpene 346900")
                    verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
                }
            }

            When("leser fil med ugyldig produksjonsdato og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_PRODDATO to readFromResource("/spk/$SPK_FEIL_UGYLDIG_PRODDATO").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_PRODDATO") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_PRODDATO, FILTILSTANDTYPE_AVV, "Prod-dato (yyyymmdd) har ugyldig format")
                    verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
                }
            }

            When("leser fil med ugyldig startrecordtype starter med 11 og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_START_RECTYPE to readFromResource("/spk/$SPK_FEIL_UGYLDIG_START_RECTYPE").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_RECTYPE") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, "Ugyldig recordtype")
                    verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
                }
            }

            When("leser fil med ugyldig endrecordtype starter med 10 og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_END_RECTYPE to readFromResource("/spk/$SPK_FEIL_UGYLDIG_END_RECTYPE").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_RECTYPE") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, "Ugyldig recordtype")
                    verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
                }
            }

            When("leser fil med ugyldig transaksjonsbeløp format og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP to readFromResource("/spk/$SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_SUMBELOP") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, "Total beløp 346900 stemmer ikke med summeringen av enkelt beløpene 0")
                    verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
                }
            }

            When("leser fil med ugyldig transaksjon-recordtype starter med 03 og parser") {
                every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE to readFromResource("/spk/$SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE").lines())
                every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
                every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
                readFileService.readAndParseFile()

                Then("skal fil info inneholde en filestatus UGYLDIG_RECTYPE") {
                    val lopeNummerFraFil = "000034"
                    val filInfoList = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(FILTILSTANDTYPE_AVV, listOf(lopeNummerFraFil))
                    verifyFilInfo(filInfoList.first(), FilStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, "Ugyldig recordtype")
                    verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
                }
            }
        }
    })

private fun verifyInntransaksjon(
    innTransaksjon: InnTransaksjon,
    filInfoId: Int,
) {
    innTransaksjon.innTransaksjonId!! shouldBeGreaterThan 0
    innTransaksjon.filInfoId shouldBe filInfoId
    innTransaksjon.transaksjonStatus shouldBe null
    innTransaksjon.fnr shouldNotBe null
    innTransaksjon.belopstype shouldBe BELOPSTYPE_SKATTEPLIKTIG_UTBETALING
    innTransaksjon.art shouldNotBe null
    innTransaksjon.avsender shouldBe SPK
    innTransaksjon.utbetalesTil shouldBe null
    innTransaksjon.datoFomStr shouldBe "20240201"
    innTransaksjon.datoTomStr shouldBe "20240229"
    innTransaksjon.datoAnviserStr shouldBe "20240131"
    innTransaksjon.belopStr shouldBe "00000346900"
    innTransaksjon.refTransId shouldBe null
    innTransaksjon.tekstkode shouldBe null
    innTransaksjon.rectype shouldBe RECTYPE_TRANSAKSJONSRECORD
    innTransaksjon.transId shouldBe "116684810"
    innTransaksjon.datoFom shouldBe LocalDate.of(2024, 2, 1)
    innTransaksjon.datoTom shouldBe LocalDate.of(2024, 2, 29)
    innTransaksjon.belop shouldBe 346900
    innTransaksjon.behandlet shouldBe BEHANDLET_NEI
    innTransaksjon.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
    innTransaksjon.opprettetAv shouldBe READ_FILE_SERVICE
    innTransaksjon.datoEndret.toLocalDate() shouldBe LocalDate.now()
    innTransaksjon.endretAv shouldBe READ_FILE_SERVICE
    innTransaksjon.versjon shouldBe 1
    innTransaksjon.grad shouldBe 100
    innTransaksjon.gradStr shouldBe "100"
}

private fun verifyLopenummer(lopeNummer: LopeNummer?) {
    lopeNummer shouldNotBe null
    lopeNummer?.let {
        it.sisteLopeNummer shouldNotBe null
        it.filType shouldBe FILTYPE_ANVISER
        it.anviser shouldBe SPK
        it.datoEndret.toLocalDate() shouldBe LocalDate.now()
        it.endretAv shouldBe READ_FILE_SERVICE
    }
}
