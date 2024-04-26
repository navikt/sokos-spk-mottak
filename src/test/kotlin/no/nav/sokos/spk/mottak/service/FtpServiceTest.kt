package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.listener.SftpListener


internal class FtpServiceTest : FunSpec({
    extensions(SftpListener)

    val ftpService: FtpService by lazy {
        FtpService(SftpConfig(SftpListener.sftpConfig).createSftpConnection())
    }

    test("opprett fil i INBOUND, flytt til FERDIG og last ned fil fra FERDIG") {
        ftpService.createFile("test.txt", Directories.INBOUND, "content")

        ftpService.moveFile("test.txt", Directories.INBOUND, Directories.FERDIG)

        val downloadedFilesFromInbound = ftpService.downloadFiles(Directories.INBOUND)
        downloadedFilesFromInbound.size shouldBe 0

        val downloadedFilesFromOutbound = ftpService.downloadFiles(Directories.FERDIG)
        downloadedFilesFromOutbound.size shouldBe 1

    }
})
