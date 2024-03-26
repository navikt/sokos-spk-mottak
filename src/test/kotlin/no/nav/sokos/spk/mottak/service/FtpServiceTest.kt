package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SftpTestConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile

internal class FtpServiceTest : FunSpec({

    val genericContainer = GenericContainer("atmoz/sftp:alpine")
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("inbound/"),
            "/home/foo/inbound"
        )
        .withCopyFileToContainer(
            MountableFile.forClasspathResource("outbound/"),
            "/home/foo/outbound"
        )
        .withPrivilegedMode(true)
        .withExposedPorts(22)
        .withCommand("foo:pass:::inbound")

    beforeEach {
        genericContainer.start()
    }

    afterEach {
        genericContainer.stop()
    }

    test("downloadFiles should return a map of filenames and content") {
        fun ftpConfig() = PropertiesConfig.SftpConfig(
            host = genericContainer.host,
            username = "foo",
            privateKeyPassword = "pass",
            port = genericContainer.getMappedPort(22)
        )

        val sftpSession = SftpTestConfig(ftpConfig()).createSftpConnection()


        val ftpService = FtpService(sftpSession)

        val files = ftpService.downloadFiles()
        files.size shouldBe 1



        ftpService.createFile("test.txt", Directories.INBOUND, "content")
        /*
        ftpService.createFile("test2.txt", Directories.INBOUND, "content2")
        val size = ftpService.downloadFiles()
        size.size shouldBe 2*/

    }

})