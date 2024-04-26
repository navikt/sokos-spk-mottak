package no.nav.sokos.spk.mottak.service

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.should
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.contain
import io.kotest.matchers.string.shouldBeBlank
import java.time.LocalDate
import kotliquery.sessionOf
import no.nav.sokos.spk.mottak.SPK_FEIL_ANTALL_TRANSAKSJONER
import no.nav.sokos.spk.mottak.SPK_FEIL_BELOPSUM
import no.nav.sokos.spk.mottak.SPK_FEIL_FORVENTET_FILLOPENUMMER
import no.nav.sokos.spk.mottak.SPK_FEIL_LOPENUMMER_DUBLETT
import no.nav.sokos.spk.mottak.SPK_FEIL_MANGLER_SLUTTRECORD
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_ANVISER
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_END_RECTYPE
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_FILTYPE
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_LOPENUMMER
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_MOTTAKER
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_PRODDATO
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_START_RECTYPE
import no.nav.sokos.spk.mottak.SPK_FEIL_UGYLDIG_TRANS_RECTYPE
import no.nav.sokos.spk.mottak.SPK_FILE_FEIL
import no.nav.sokos.spk.mottak.SPK_FILE_OK
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.domain.BEHANDLET_NEI
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_AVV
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.Lopenummer
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.Db2Listener.dataSource
import no.nav.sokos.spk.mottak.listener.Db2Listener.lopenummerRepository
import no.nav.sokos.spk.mottak.listener.SftpListener
import no.nav.sokos.spk.mottak.validator.FileStatus

private const val SYSTEM_ID = "sokos-spk-mottak"

class FileReaderServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, SftpListener))

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpConfig).createSftpConnection())
    }

    val fileReaderService: FileReaderService by lazy {
        FileReaderService(dataSource, ftpService)
    }

    afterEach {
        ftpService.deleteFile(
            Directories.INBOUND.value + "/SPK_NAV_*",
            Directories.FERDIG.value + "/SPK_NAV_*",
            Directories.ANVISNINGSRETUR.value + "/SPK_NAV_*"
        )
    }

    Given("det finnes en ubehandlet fil i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(SPK_FILE_OK, Directories.INBOUND, SPK_FILE_OK.readFromResource())
        When("leser filen på FTP-serveren og lagre dataene.") {
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren og transaksjoner blir lagret i databasen.") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1

                val sisteLopenummer = 34
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_GOD)
                verifyFilInfo(filInfo, FileStatus.OK, FILTILSTANDTYPE_GOD)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.size shouldBe 8
                verifyInntransaksjon(inntransaksjonList.first(), filInfo.filInfoId!!)
            }
        }
    }

    Given("det finnes to ubehandlede filer i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(SPK_FILE_OK, Directories.INBOUND, SPK_FILE_OK.readFromResource())
        ftpService.createFile(SPK_FILE_FEIL, Directories.INBOUND, SPK_FILE_FEIL.readFromResource())
        When("leser filene på FTP-serveren og lagrer dataene og en av filene har feil i totalbeløp") {
            lopenummerRepository.updateLopenummer(33, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal begge filene bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, transaksjoner blir lagret i databasen og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 2

                val sisteLopenummer = 35
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_AVV)
                val feiltekst = "Total beløp 2775100 stemmer ikke med summeringen av enkelt beløpene 2775200"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med beløpsfeil i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_BELOPSUM,
            Directories.INBOUND,
            SPK_FEIL_BELOPSUM.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(35, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 36
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_AVV)
                val feiltekst = "Total beløp 2775100 stemmer ikke med summeringen av enkelt beløpene 346900"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med feil i antall transaksjoner i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_ANTALL_TRANSAKSJONER,
            Directories.INBOUND,
            SPK_FEIL_ANTALL_TRANSAKSJONER.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(36, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 37
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_AVV)
                val feiltekst = "Oppsumert antall records 8 stemmer ikke med det faktiske antallet 1"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_ANTRECORDS, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med allerede brukt løpenummer i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_LOPENUMMER_DUBLETT,
            Directories.INBOUND,
            SPK_FEIL_LOPENUMMER_DUBLETT.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(34, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 34
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_AVV)
                val feiltekst = "Filløpenummer 34 allerede i bruk"
                verifyFilInfo(filInfo, FileStatus.FILLOPENUMMER_I_BRUK, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med et ugyldig løpenummer i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_FORVENTET_FILLOPENUMMER,
            Directories.INBOUND,
            SPK_FEIL_FORVENTET_FILLOPENUMMER.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(37, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 37
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(99, FILTILSTANDTYPE_AVV)
                val feiltekst = "Forventet lopenummer 38"
                verifyFilInfo(filInfo, FileStatus.FORVENTET_FILLOPENUMMER, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig prod-dato i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_PRODDATO,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_PRODDATO.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(37, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 38
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_AVV)
                val feiltekst = "Prod-dato (yyyymmdd) har ugyldig format"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_PRODDATO, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig transaksjon-recordtype i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_TRANS_RECTYPE,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_TRANS_RECTYPE.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(38, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 39
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_AVV)
                val feiltekst = "Ugyldig recordtype"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig startrecordtype i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_START_RECTYPE,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_START_RECTYPE.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(39, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 40
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_AVV)
                val feiltekst = "Ugyldig recordtype"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig endrecordtype i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_END_RECTYPE,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_END_RECTYPE.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(40, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 41
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_AVV)
                val feiltekst = "Ugyldig recordtype"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig løpenummer i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_LOPENUMMER,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_LOPENUMMER.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(41, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 41
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(0, FILTILSTANDTYPE_AVV)
                val feiltekst = "Filløpenummer format er ikke gyldig"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_FILLOPENUMMER, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig mottaker  i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_MOTTAKER,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_MOTTAKER.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(41, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 42
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer, FILTILSTANDTYPE_AVV)
                val feiltekst = "Ugyldig mottaker"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_MOTTAKER, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }


    Given("det finnes en ubehandlet fil med ugyldig filtype i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_FILTYPE,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_FILTYPE.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(42, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 42
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(43, FILTILSTANDTYPE_AVV)
                val feiltekst = "Ugyldig filtype"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_FILTYPE, FILTILSTANDTYPE_AVV, feiltekst, "ANX", "SPK")

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig anviser i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_ANVISER,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_ANVISER.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(42, FILETYPE_ANVISER, sessionOf(dataSource))
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren, ingen av transaksjonene blir lagret og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
                val sisteLopenummer = 42
                val lopenummer = lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(43, FILTILSTANDTYPE_AVV, "SPX")
                val feiltekst = "Ugyldig anviser"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_ANVISER, FILTILSTANDTYPE_AVV, feiltekst, "ANV", "SPX")

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getByFilInfoId(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med manglende sluttrecord i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(
            SPK_FEIL_MANGLER_SLUTTRECORD,
            Directories.INBOUND,
            SPK_FEIL_MANGLER_SLUTTRECORD.readFromResource()
        )
        When("leser filen på FTP-serveren og lagrer dataene.") {
            lopenummerRepository.updateLopenummer(42, FILETYPE_ANVISER, sessionOf(dataSource))
            val exception = shouldThrow<Exception> {
                fileReaderService.readAndParseFile()
            }

            Then("skal det kastes en exception med feilmelding om ukjent feil, ingen av transaksjonene blir lagret, filen blir ikke flyttet til \"inbound/ferdig\"  og ingen avviksfil blir opprettet") {
                exception.message should contain("Ukjent feil ved innlesing av fil: SPK_NAV_20372503_080026814_ANV.txt")
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 0
                val sisteLopenummer = lopenummerRepository.findMaxLopenummer(FILETYPE_ANVISER)
                sisteLopenummer shouldBe 42

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(44, FILTILSTANDTYPE_AVV)
                filInfo shouldBe null
                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 0
            }
        }
    }
})

private fun verifyInntransaksjon(innTransaksjon: InnTransaksjon, filInfoId: Int) {
    innTransaksjon.innTransaksjonId!! shouldBeGreaterThan 0
    innTransaksjon.filInfoId shouldBe filInfoId
    innTransaksjon.transaksjonStatus shouldBe null
    innTransaksjon.fnr shouldNotBe null
    innTransaksjon.belopstype shouldBe BELOPTYPE_SKATTEPLIKTIG_UTBETALING
    innTransaksjon.art shouldNotBe null
    innTransaksjon.avsender shouldBe SPK
    innTransaksjon.utbetalesTil.shouldBeBlank()
    innTransaksjon.datoFomStr shouldBe "20240201"
    innTransaksjon.datoTomStr shouldBe "20240229"
    innTransaksjon.datoAnviserStr shouldBe "20240131"
    innTransaksjon.belopStr shouldBe "00000346900"
    innTransaksjon.refTransId.shouldBeBlank()
    innTransaksjon.tekstkode.shouldBeBlank()
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
    innTransaksjon.prioritetStr.shouldBeBlank()
    innTransaksjon.trekkansvar.shouldBeBlank()
    innTransaksjon.saldoStr shouldBe "00000000410"
    innTransaksjon.kid.shouldBeBlank()
    innTransaksjon.prioritet shouldBe null
    innTransaksjon.saldo shouldBe 410
    innTransaksjon.grad shouldBe null
    innTransaksjon.gradStr.shouldBeBlank()
}

private fun verifyLopenummer(lopenummer: Lopenummer?) {
    lopenummer shouldNotBe null
    lopenummer?.let {
        it.sisteLopenummer shouldNotBe null
        it.filType shouldBe FILETYPE_ANVISER
        it.anviser shouldBe SPK
        it.datoEndret.toLocalDate() shouldBe LocalDate.now()
        it.endretAv shouldBe SYSTEM_ID
    }
}

private fun verifyFilInfo(
    filInfo: FilInfo?,
    fileStatus: FileStatus,
    filTilstandType: String,
    feiltekst: String? = null,
    fileType: String = FILETYPE_ANVISER,
    anviser: String = SPK
) {
    filInfo shouldNotBe null
    filInfo?.let {
        it.filInfoId shouldNotBe null
        it.filStatus shouldBe fileStatus.code
        it.anviser shouldBe anviser
        it.filType shouldBe fileType
        it.filTilstandType shouldBe filTilstandType
        it.filNavn shouldNotBe null
        it.lopenr shouldNotBe null
        it.feiltekst shouldBe feiltekst
        it.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
        it.opprettetAv shouldBe SYSTEM_ID
        it.datoSendt shouldBe null
        it.datoEndret.toLocalDate() shouldBe LocalDate.now()
        it.endretAv shouldBe SYSTEM_ID
        it.versjon shouldBe 1
    }
}
