package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.listener.SftpListener

internal class FtpServiceTest :
    FunSpec({
        extensions(SftpListener)

        val ftpService: FtpService by lazy {
            FtpService(SftpConfig(SftpListener.sftpProperties))
        }

        test("opprett fil i INBOUND, flytt til FERDIG og last ned fil fra FERDIG") {
            ftpService.createFile("test.txt", Directories.INBOUND, "content")
            ftpService.downloadFiles(Directories.INBOUND).size shouldBe 1

            ftpService.moveFile("test.txt", Directories.INBOUND, Directories.FERDIG)
            ftpService.downloadFiles(Directories.INBOUND).size shouldBe 0
            ftpService.downloadFiles(Directories.FERDIG).size shouldBe 1
        }
    })
