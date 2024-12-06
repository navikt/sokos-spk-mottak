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

        test("opprett 3 filer i INBOUND, last ned filer til INBOUND og sjekk at de er listet i sortert rekkefølge") {
            ftpService.createFile("P611.ANV.NAV.HVD.SPK.L004009.D240701.T185923", Directories.INBOUND, "content1")
            ftpService.createFile("P611.ANV.NAV.ETT.SPK.L004001.D240701.T190116", Directories.INBOUND, "content2")
            ftpService.createFile("P611.ANV.NAV.HUB.SPK.L004005.D240907.T004221", Directories.INBOUND, "content3")
            val downloadFiles = ftpService.downloadFiles(Directories.INBOUND)
            downloadFiles.size shouldBe 3
            downloadFiles.keys.toList() shouldBe
                listOf(
                    "P611.ANV.NAV.ETT.SPK.L004001.D240701.T190116",
                    "P611.ANV.NAV.HUB.SPK.L004005.D240907.T004221",
                    "P611.ANV.NAV.HVD.SPK.L004009.D240701.T185923",
                )
        }
    })
