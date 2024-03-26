package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SftpTestConfig
import org.testcontainers.containers.GenericContainer

internal class FtpServiceTest : FunSpec({

    val genericContainer = GenericContainer("atmoz/sftp:alpine-3.7")
        .withExposedPorts(22)
        .withCommand("foo:pass:::inbound,inbound/ferdig,outbound")

    fun ftpConfig() = PropertiesConfig.SftpConfig(
        host = genericContainer.host,
        username = "foo",
        privateKeyPassword = "pass",
        port = genericContainer.getMappedPort(22)
    )


    beforeTest {
        genericContainer.start()
    }

    afterTest {
        genericContainer.stop()
    }

    test("tester å lage fil i inbound og hente den ut igjen") {

        val sftpSession = SftpTestConfig(ftpConfig()).createSftpConnection()

        val ftpService = FtpService(sftpSession)

        ftpService.createFile("test.txt", Directories.INBOUND, "content")
        val files = ftpService.downloadFiles()
        files.size shouldBe 1

    }

    test("tester å flytte fil fra inbound til outbound") {

        val sftpSession = SftpTestConfig(ftpConfig()).createSftpConnection()

        val ftpService = FtpService(sftpSession)

        ftpService.createFile("test.txt", Directories.INBOUND, "content")
        val files = ftpService.downloadFiles()
        files.size shouldBe 1

        ftpService.moveFile("test.txt", Directories.INBOUND, Directories.FERDIG)
        val filesAfterMoveInbound = ftpService.downloadFiles(Directories.INBOUND)
        filesAfterMoveInbound.size shouldBe 0
        val filesAfterMoveOutbound = ftpService.downloadFiles(Directories.FERDIG)
        filesAfterMoveOutbound.size shouldBe 1

    }

})