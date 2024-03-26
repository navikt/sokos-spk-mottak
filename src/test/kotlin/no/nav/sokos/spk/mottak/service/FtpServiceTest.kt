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
            MountableFile.forClasspathResource("files/"),
            "/home/foo/inbound/files"
        )
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
            keyPass = "pass",
            privKey = "privKey",
            port = 22
        )

        val sftpSession = SftpTestConfig(ftpConfig()).createSftpConnection()

        try {
            val ftpService = FtpService(sftpSession) // TODO: Add ftpConfig as parameter
            val files = ftpService.downloadFiles()
            files.size shouldBe 1
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

})

