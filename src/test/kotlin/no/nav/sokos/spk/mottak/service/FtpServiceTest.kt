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

    test("test oppretting av fil i inbound og hente den ut") {
        ftpService.createFile("test.txt", Directories.INBOUND, "content")

        val downloadedFiles = ftpService.downloadFiles()
        downloadedFiles.size shouldBe 1

    }

    test("test flytting av fil fra inbound til outbound") {
        ftpService.createFile("test.txt", Directories.INBOUND, "content")

        val downloadedFiles = ftpService.downloadFiles()
        downloadedFiles.size shouldBe 1

        ftpService.moveFile("test.txt", Directories.INBOUND, Directories.FERDIG)

        val downloadedFilesFromInbound = ftpService.downloadFiles(Directories.INBOUND)
        downloadedFilesFromInbound.size shouldBe 0

        val downloadedFilesFromOutbound = ftpService.downloadFiles(Directories.FERDIG)
        downloadedFilesFromOutbound.size shouldBe 1

    }
})
