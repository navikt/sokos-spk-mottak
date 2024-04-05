package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.sokos.spk.mottak.SPK_FILE_OK
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.SftpListener

private const val SISTE_LOPENUMMER = 34

class FileReaderServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener, SftpListener))

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpConfig).createSftpConnection())
    }

    val fileReaderService: FileReaderService by lazy {
        FileReaderService(Db2Listener.dataSource, ftpService)
    }

    given("det finnes en ubehandlet fil i \"inbound\" på FTP-serveren ") {
        ftpService.createFile(SPK_FILE_OK, Directories.INBOUND, SPK_FILE_OK.readFromResource())
        `when`("les filen på FTP-serveren og lagre dataene.") {
            fileReaderService.readAndParseFile()

            then("skal filen blir flyttet fra \"inbound\" til \"inbound/ferdig\" på FTP-serveren og transaksjoner blir lagret i database.") {
                val downloadedFilesFromOutbound = ftpService.downloadFiles(Directories.FERDIG)
                downloadedFilesFromOutbound.size shouldBe 1

                val lopenummer = Db2Listener.lopenummerRepository.getLopenummer(SISTE_LOPENUMMER)
                lopenummer shouldNotBe null

            }
        }
    }
})