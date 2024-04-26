package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldBeBlank
import java.time.LocalDate
import kotliquery.sessionOf
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
import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_AVV
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.Lopenummer
import no.nav.sokos.spk.mottak.domain.RECTYPE_TRANSAKSJONSRECORD
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.SftpListener
import no.nav.sokos.spk.mottak.validator.FileStatus

private const val SYSTEM_ID = "sokos-spk-mottak"
private const val MAX_LOPENUMMER = 33

class FileReaderServiceTest : BehaviorSpec({

    extensions(listOf(Db2Listener, SftpListener))

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpConfig).createSftpConnection())
    }

    val fileReaderService: FileReaderService by lazy {
        FileReaderService(Db2Listener.dataSource, ftpService)
    }

    afterEach {
        Db2Listener.lopenummerRepository.updateLopenummer(MAX_LOPENUMMER, FILETYPE_ANVISER, sessionOf(Db2Listener.dataSource))
        Db2Listener.dataSource.connection.createStatement().execute("DELETE FROM T_FIL_INFO")
    }


    Given("det finnes en ubehandlet fil i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(SPK_FILE_OK, Directories.INBOUND, SPK_FILE_OK.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal filen bli flyttet fra \"inbound\" til \"inbound/ferdig\" og transaksjoner blir lagret i databasen.") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1

                val lopeNummerFraFil = 34
                val lopenummer = Db2Listener.lopenummerRepository.getLopenummer(lopeNummerFraFil)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_GOD)
                verifyFilInfo(filInfo, FileStatus.OK, FILTILSTANDTYPE_GOD)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getInnTransaksjoner(filInfo?.filInfoId!!)
                inntransaksjonList.size shouldBe 8
                verifyInntransaksjon(inntransaksjonList.first(), filInfo.filInfoId!!)

                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
            }
        }
    }

    Given("det finnes to ubehandlede filer i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(SPK_FILE_OK, Directories.INBOUND, SPK_FILE_OK.readFromResource())
        ftpService.createFile(SPK_FILE_FEIL, Directories.INBOUND, SPK_FILE_FEIL.readFromResource())

        When("leser begge filene og parser") {
            fileReaderService.readAndParseFile()

            Then("skal begge filene bli flyttet fra \"inbound\" til \"inbound/ferdig\", transaksjoner blir lagret i databasen og en avviksfil blir opprettet i \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 2

                val lopeNummerFraFil = 35
                val lopenummer = Db2Listener.lopenummerRepository.getLopenummer(lopeNummerFraFil)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                val feiltekst = "Total beløp 2775100 stemmer ikke med summeringen av enkelt beløpene 2775200"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getInnTransaksjoner(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig anviser") {
        ftpService.createFile(SPK_FEIL_UGYLDIG_ANVISER, Directories.INBOUND, SPK_FEIL_UGYLDIG_ANVISER.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_ANVISER") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_ANVISER, FILTILSTANDTYPE_AVV, "Ugyldig anviser")
                Db2Listener.lopenummerRepository.findMaxLopenummer(FILETYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }
    }

    Given("det finnes ubehandlet fil med ugyldig mottaker") {
        ftpService.createFile(SPK_FEIL_UGYLDIG_MOTTAKER, Directories.INBOUND, SPK_FEIL_UGYLDIG_MOTTAKER.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_MOTTAKER") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_MOTTAKER, FILTILSTANDTYPE_AVV, "Ugyldig mottaker")
                verifyLopenummer(Db2Listener.lopenummerRepository.getLopenummer(lopeNummerFraFil))
            }
        }
    }

    Given("det finnes ubehandlet fil med filløpenummer i bruk") {
        ftpService.createFile(SPK_FEIL_FILLOPENUMMER_I_BRUK, Directories.INBOUND, SPK_FEIL_FILLOPENUMMER_I_BRUK.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus FILLOPENUMMER_I_BRUK") {
                val lopeNummerFraFil = 32
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.FILLOPENUMMER_I_BRUK, FILTILSTANDTYPE_AVV, "Filløpenummer $lopeNummerFraFil allerede i bruk")
                Db2Listener.lopenummerRepository.findMaxLopenummer(FILETYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }
    }

    Given("det finnes ubehandlet fil med ugyldig løpenummer") {
        ftpService.createFile(SPK_FEIL_UGYLDIG_LOPENUMMER, Directories.INBOUND, SPK_FEIL_UGYLDIG_LOPENUMMER.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_FILLOPENUMMER") {
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(0, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_FILLOPENUMMER, FILTILSTANDTYPE_AVV, "Filløpenummer format er ikke gyldig")
                Db2Listener.lopenummerRepository.findMaxLopenummer(FILETYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }
    }

    Given("det finnes ubehandlet fil med feil forventet løpenummer") {
        ftpService.createFile(SPK_FEIL_FORVENTET_FILLOPENUMMER, Directories.INBOUND, SPK_FEIL_FORVENTET_FILLOPENUMMER.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus FORVENTET_FILLOPENUMMER") {
                val lopeNummerFraFil = 99
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.FORVENTET_FILLOPENUMMER, FILTILSTANDTYPE_AVV, "Forventet lopenummer 34")
                Db2Listener.lopenummerRepository.findMaxLopenummer(FILETYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }
    }

    Given("det finnes ubehandlet fil som har ugyldig filtype") {
        ftpService.createFile(SPK_FEIL_UGYLDIG_FILTYPE, Directories.INBOUND, SPK_FEIL_UGYLDIG_FILTYPE.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_FILTYPE") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_FILTYPE, FILTILSTANDTYPE_AVV, "Ugyldig filtype", "ANX", "SPK")
                Db2Listener.lopenummerRepository.findMaxLopenummer(FILETYPE_ANVISER) shouldBe MAX_LOPENUMMER
            }
        }
    }

    Given("det finnes ubehandlet fil med ugydlig antall records") {
        ftpService.createFile(SPK_FEIL_UGYLDIG_ANTRECORDS, Directories.INBOUND, SPK_FEIL_UGYLDIG_ANTRECORDS.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_ANTRECORDS") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_ANTRECORDS, FILTILSTANDTYPE_AVV, "Oppsumert antall records 8 stemmer ikke med det faktiske antallet 1")
                verifyLopenummer(Db2Listener.lopenummerRepository.getLopenummer(lopeNummerFraFil))
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig sumbeløp") {
        ftpService.createFile(SPK_FEIL_UGYLDIG_SUMBELOP, Directories.INBOUND, SPK_FEIL_UGYLDIG_SUMBELOP.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_SUMBELOP") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, "Total beløp 2775100 stemmer ikke med summeringen av enkelt beløpene 346900")
                verifyLopenummer(Db2Listener.lopenummerRepository.getLopenummer(lopeNummerFraFil))
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig produsert dato") {
        ftpService.createFile(SPK_FEIL_UGYLDIG_PRODDATO, Directories.INBOUND, SPK_FEIL_UGYLDIG_PRODDATO.readFromResource())

        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_PRODDATO") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_PRODDATO, FILTILSTANDTYPE_AVV, "Prod-dato (yyyymmdd) har ugyldig format")
                verifyLopenummer(Db2Listener.lopenummerRepository.getLopenummer(lopeNummerFraFil))
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig startrecordtype starter med 11") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_START_RECTYPE,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_START_RECTYPE.readFromResource()
        )
        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_RECTYPE") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, "Ugyldig recordtype")
                verifyLopenummer(Db2Listener.lopenummerRepository.getLopenummer(lopeNummerFraFil))
            }
        }
    }

    Given("det finnes en ubehandlet fil med ugyldig endrecordtype starter med 10") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_END_RECTYPE,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_END_RECTYPE.readFromResource()
        )
        When("leser filen og parser") {
            fileReaderService.readAndParseFile()

            Then("skal fil info inneholde en filestatus UGYLDIG_RECTYPE") {
                val lopeNummerFraFil = 34
                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(lopeNummerFraFil, FILTILSTANDTYPE_AVV)
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_RECTYPE, FILTILSTANDTYPE_AVV, "Ugyldig recordtype")
                verifyLopenummer(Db2Listener.lopenummerRepository.getLopenummer(lopeNummerFraFil))
            }
        }
    }

    // TODO: Fix test
    Given("det finnes en ubehandlet fil med ugyldig transaksjonsbeløp") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_TRANSAKSJONS_BELOP.readFromResource()
        )
    }


    // TODO: Fix test
    Given("det finnes en ubehandlet fil med ugyldig transaksjon-recordtype starter med 03") {
        ftpService.createFile(
            SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE,
            Directories.INBOUND,
            SPK_FEIL_UGYLDIG_TRANSAKSJON_RECTYPE.readFromResource()
        )

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
    innTransaksjon.tekstKode.shouldBeBlank()
    innTransaksjon.recType shouldBe RECTYPE_TRANSAKSJONSRECORD
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
