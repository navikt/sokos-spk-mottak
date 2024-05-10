package no.nav.sokos.spk.mottak.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
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
import no.nav.sokos.spk.mottak.SPK_FILE_OK
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.domain.BEHANDLET_NEI
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_AVV
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.FILTYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.LopeNummer
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.Db2Listener.dataSource
import no.nav.sokos.spk.mottak.listener.SftpListener
import java.io.IOException
import java.time.LocalDate

private const val SYSTEM_ID = "sokos-spk-mottak"
private const val MAX_LOPENUMMER = 33

class ReadParseFileServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, SftpListener))

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpConfig).createSftpConnection())
    }

    val readAndParseFileService: ReadAndParseFileService by lazy {
        ReadAndParseFileService(dataSource, ftpService)
    }

    afterEach {
        SftpListener.deleteFile(
            Directories.INBOUND.value + "/SPK_NAV_*",
            Directories.FERDIG.value + "/SPK_NAV_*",
            Directories.ANVISNINGSRETUR.value + "/SPK_NAV_*",
        )
    }

    Given("det finnes en ubehandlet fil i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(SPK_FILE_OK, Directories.INBOUND, readFromResource("/spk/$SPK_FILE_OK"))

        When("leser filen og parser") {
            readAndParseFileService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" og transaksjoner blir lagret i databasen.") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1

                val lopeNummerFraFil = 34
                val lopeNummer = Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil)
                verifyLopenummer(lopeNummer)

                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_GOD)
                verifyFilInfo(filInfo, FilStatus.OK, FILTILSTANDTYPE_GOD)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.size shouldBe 8
                verifyInntransaksjon(inntransaksjonList.first(), filInfo.filInfoId!!)
            }
        }
    }

    Given("det finnes to ubehandlede filer i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(SPK_FILE_OK, Directories.INBOUND, readFromResource("/spk/$SPK_FILE_OK"))
        ftpService.createFile(SPK_FILE_FEIL, Directories.INBOUND, readFromResource("/spk/$SPK_FILE_FEIL"))

        When("leser begge filene og parser") {
            readAndParseFileService.readAndParseFile()

            Then("skal begge filene bli flyttet fra \"inbound\" til \"inbound/ferdig\", transaksjoner blir lagret i databasen og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 2
                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1

                val lopeNummerFraFil = 35
                val lopenummer = Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                val feiltekst = "Total beløp 2775100 stemmer ikke med summeringen av enkelt beløpene 2775200"
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()
            }
        }
    }

    @Suppress("NAME_SHADOWING")
    Given("det finnes ubehandlet fil i \"inbound\" på FTP-serveren ") {
        val ftpServiceMock = mockk<FtpService>()
        val readAndParseFileService: ReadAndParseFileService by lazy {
            ReadAndParseFileService(dataSource, ftpServiceMock)
        }

        When("leser ok format filen og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FILE_OK to readFromResource("/spk/$SPK_FILE_OK").lines())

            Then("skal det kastet en MottakException med uforventet feil") {
                every { ftpServiceMock.moveFile(any(), any(), any()) } throws IOException("Ftp server is down!")
                val exception =
                    shouldThrow<MottakException> {
                        readAndParseFileService.readAndParseFile()
                    }
                exception.message shouldBe "Ukjent feil ved innlesing av fil: SPK_NAV_20242503_070026814_ANV_OK.txt. Feilmelding: Ftp server is down!"
            }
        }

        When("leser feil format filen og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FILE_FEIL to readFromResource("/spk/$SPK_FILE_FEIL").lines())

            Then("skal det kastet en MottakException med uforventet feil") {
                every { ftpServiceMock.createFile(any(), any(), any()) } throws IOException("Ftp server can not move file!")
                val exception =
                    shouldThrow<MottakException> {
                        readAndParseFileService.readAndParseFile()
                    }
                exception.message shouldBe "Feil ved opprettelse av avviksfil: SPK_NAV_20242503_080026814_ANV_FEIL.txt. Feilmelding: Ftp server can not move file!"
            }
        }

        When("leser fil med ugyldig anviser og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_ANVISER to readFromResource("/spk/$SPK_FEIL_UGYLDIG_ANVISER").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_ANVISER") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_ANVISER, FILTILSTANDTYPE_AVV, "Ugyldig anviser")
                Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }

        When("leser fil med ugyldig mottaker og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_MOTTAKER to readFromResource("/spk/$SPK_FEIL_UGYLDIG_MOTTAKER").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_MOTTAKER") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_MOTTAKER, FILTILSTANDTYPE_AVV, "Ugyldig mottaker")
                verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
            }
        }

        When("leser fil med løpenummer som er i bruk og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_FILLOPENUMMER_I_BRUK to readFromResource("/spk/$SPK_FEIL_FILLOPENUMMER_I_BRUK").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus FILLOPENUMMER_I_BRUK") {
                val lopeNummerFraFil = 32
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.FILLOPENUMMER_I_BRUK, FILTILSTANDTYPE_AVV, "Filløpenummer $lopeNummerFraFil allerede i bruk")
                Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }

        When("leser fil med ugyldig løpenummer og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_LOPENUMMER to readFromResource("/spk/$SPK_FEIL_UGYLDIG_LOPENUMMER").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_FILLOPENUMMER") {
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(0, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_FILLOPENUMMER, FILTILSTANDTYPE_AVV, "Filløpenummer format er ikke gyldig")
                Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }

        When("leser fil med ikke forventet løpenummer og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_FORVENTET_FILLOPENUMMER to readFromResource("/spk/$SPK_FEIL_FORVENTET_FILLOPENUMMER").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus FORVENTET_FILLOPENUMMER") {
                val lopeNummerFraFil = 99
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.FORVENTET_FILLOPENUMMER, FILTILSTANDTYPE_AVV, "Forventet lopenummer 34")
                Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }

        When("leser fil med ugyldig filtype og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_FILTYPE to readFromResource("/spk/$SPK_FEIL_UGYLDIG_FILTYPE").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_FILTYPE") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_FILTYPE, FILTILSTANDTYPE_AVV, "Ugyldig filtype", "ANX", "SPK")
                Db2Listener.lopeNummerRepository.findMaxLopeNummer(FILTYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }

        When("leser fil med ugyldig antall record og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_ANTRECORDS to readFromResource("/spk/$SPK_FEIL_UGYLDIG_ANTRECORDS").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_ANTRECORDS") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_ANTRECORDS, FILTILSTANDTYPE_AVV, "Oppsumert antall records 8 stemmer ikke med det faktiske antallet 1")
                verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
            }
        }

        When("leser fil med ugyldig sumbeløp og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_SUMBELOP to readFromResource("/spk/$SPK_FEIL_UGYLDIG_SUMBELOP").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_SUMBELOP") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, "Total beløp 2775100 stemmer ikke med summeringen av enkelt beløpene 346900")
                verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
            }
        }

        When("leser fil med ugyldig produksjonsdato og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_PRODDATO to readFromResource("/spk/$SPK_FEIL_UGYLDIG_PRODDATO").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_PRODDATO") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_PRODDATO, FILTILSTANDTYPE_AVV, "Prod-dato (yyyymmdd) har ugyldig format")
                verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
            }
        }

        When("leser fil med ugyldig startrecordtype starter med 11 og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_START_RECTYPE to readFromResource("/spk/$SPK_FEIL_UGYLDIG_START_RECTYPE").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_RECTYPE") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, "Ugyldig recordtype")
                verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
            }
        }

        When("leser fil med ugyldig endrecordtype starter med 10 og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_END_RECTYPE to readFromResource("/spk/$SPK_FEIL_UGYLDIG_END_RECTYPE").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_RECTYPE") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, "Ugyldig recordtype")
                verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
            }
        }

        When("leser fil med ugyldig transaksjonsbeløp format og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP to readFromResource("/spk/$SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_SUMBELOP") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, "Total beløp 346900 stemmer ikke med summeringen av enkelt beløpene 0")
                verifyLopenummer(Db2Listener.lopeNummerRepository.getLopeNummer(lopeNummerFraFil))
            }
        }

        When("leser fil med ugyldig transaksjon-recordtype starter med 03 og parser") {
            every { ftpServiceMock.downloadFiles() } returns mapOf(SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE to readFromResource("/spk/$SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE").lines())
            every { ftpServiceMock.createFile(any(), any(), any()) } returns Unit
            every { ftpServiceMock.moveFile(any(), any(), any()) } returns Unit
            readAndParseFileService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_RECTYPE") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.filInfoRepository.getByLopenummerAndFilTilstand(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FilStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, "Ugyldig recordtype")
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
    innTransaksjon.belopstype shouldBe BELOPTYPE_SKATTEPLIKTIG_UTBETALING
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
    innTransaksjon.opprettetAv shouldBe SYSTEM_ID
    innTransaksjon.datoEndret.toLocalDate() shouldBe LocalDate.now()
    innTransaksjon.endretAv shouldBe SYSTEM_ID
    innTransaksjon.versjon shouldBe 1
    innTransaksjon.grad shouldBe null
    innTransaksjon.gradStr shouldBe null
}

private fun verifyLopenummer(lopeNummer: LopeNummer?) {
    lopeNummer shouldNotBe null
    lopeNummer?.let {
        it.sisteLopeNummer shouldNotBe null
        it.filType shouldBe FILTYPE_ANVISER
        it.anviser shouldBe SPK
        it.datoEndret.toLocalDate() shouldBe LocalDate.now()
        it.endretAv shouldBe SYSTEM_ID
    }
}

private fun verifyFilInfo(
    filInfo: FilInfo?,
    filStatus: FilStatus,
    filTilstandType: String,
    feilTekst: String? = null,
    fileType: String = FILTYPE_ANVISER,
    anviser: String = SPK,
) {
    filInfo shouldNotBe null
    filInfo?.let {
        it.filInfoId shouldNotBe null
        it.filStatus shouldBe filStatus.code
        it.anviser shouldBe anviser
        it.filType shouldBe fileType
        it.filTilstandType shouldBe filTilstandType
        it.filNavn shouldNotBe null
        it.lopeNr shouldNotBe null
        it.feilTekst shouldBe feilTekst
        it.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
        it.opprettetAv shouldBe SYSTEM_ID
        it.datoSendt shouldBe null
        it.datoEndret.toLocalDate() shouldBe LocalDate.now()
        it.endretAv shouldBe SYSTEM_ID
        it.versjon shouldBe 1
    }
}
