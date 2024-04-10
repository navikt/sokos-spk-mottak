package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldBeBlank
import java.time.LocalDate
import kotliquery.sessionOf
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

private const val LOPENUMMER = 34
private const val SYSTEM_ID = "sokos-spk-mottak"

class FileReaderServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, SftpListener))

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpConfig).createSftpConnection())
    }

    val fileReaderService: FileReaderService by lazy {
        FileReaderService(Db2Listener.dataSource, ftpService)
    }

    afterEach {
        Db2Listener.lopenummerRepository.updateLopenummer(33, FILETYPE_ANVISER, sessionOf(Db2Listener.dataSource))
    }

    Given("det finnes en ubehandlet fil i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(SPK_FILE_OK, Directories.INBOUND, SPK_FILE_OK.readFromResource())
        When("leser filen på FTP-serveren og lagre dataene.") {
            fileReaderService.readAndParseFile()

            Then("skal filen blir flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren og transaksjoner blir lagret i database.") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1

                val lopenummer = Db2Listener.lopenummerRepository.getLopenummer(LOPENUMMER)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(LOPENUMMER)
                verifyFilInfo(filInfo, FileStatus.OK, FILTILSTANDTYPE_GOD)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getInnTransaksjoner(filInfo?.filInfoId!!)
                inntransaksjonList.size shouldBe 8
                verifyInntransaksjon(inntransaksjonList.first(), filInfo.filInfoId!!)

                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
            }
        }
    }

    Given("det finnes flere ubehandlet fil i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(SPK_FILE_OK, Directories.INBOUND, SPK_FILE_OK.readFromResource())
        ftpService.createFile(SPK_FILE_FEIL, Directories.INBOUND, SPK_FILE_FEIL.readFromResource())
        When("leser filen på FTP-serveren og lagre dataene.") {
            fileReaderService.readAndParseFile()

            Then("skal begge filene blir flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren og transaksjoner blir lagret i database. Og en avviksfil blir opprettet under \"inbound\\anvisningsretur\"") {
                ftpService.downloadFiles(Directories.FERDIG).size shouldBe 2

                val sisteLopenummer = 35
                val lopenummer = Db2Listener.lopenummerRepository.getLopenummer(sisteLopenummer)
                verifyLopenummer(lopenummer)

                val filInfo = Db2Listener.fileInfoRepository.getFileInfo(sisteLopenummer)
                val feiltekst = "Total beløp 2775200 stemmer ikke med summeringen av enkelt beløpene"
                verifyFilInfo(filInfo, FileStatus.UGYLDIG_SUMBELOP, FILTILSTANDTYPE_AVV, feiltekst)

                val inntransaksjonList = Db2Listener.innTransaksjonRepository.getInnTransaksjoner(filInfo?.filInfoId!!)
                inntransaksjonList.shouldBeEmpty()

                ftpService.downloadFiles(Directories.ANVISNINGSRETUR).size shouldBe 1
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

private fun verifyFilInfo(filInfo: FilInfo?, fileStatus: FileStatus, filTilstandType: String, feiltekst: String? = null) {
    filInfo shouldNotBe null
    filInfo?.let {
        it.filInfoId shouldNotBe null
        it.filStatus shouldBe fileStatus.code
        it.anviser shouldBe SPK
        it.filType shouldBe FILETYPE_ANVISER
        it.filTilstandType shouldBe filTilstandType
        it.filNavn shouldNotBe null
        it.lopenr shouldNotBe null
        it.feiltekst shouldBe feiltekst
        it.datoMottatt shouldNotBe null
        it.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
        it.opprettetAv shouldBe SYSTEM_ID
        it.datoSendt shouldBe null
        it.datoEndret.toLocalDate() shouldBe LocalDate.now()
        it.endretAv shouldBe SYSTEM_ID
        it.versjon shouldBe 1
    }
}