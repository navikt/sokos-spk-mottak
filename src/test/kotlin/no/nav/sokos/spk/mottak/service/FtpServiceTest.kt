package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64.getEncoder
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SftpConfig
import org.testcontainers.containers.GenericContainer
import org.testcontainers.images.builder.Transferable
import org.testcontainers.shaded.org.bouncycastle.crypto.AsymmetricCipherKeyPair
import org.testcontainers.shaded.org.bouncycastle.crypto.generators.Ed25519KeyPairGenerator
import org.testcontainers.shaded.org.bouncycastle.crypto.params.AsymmetricKeyParameter
import org.testcontainers.shaded.org.bouncycastle.crypto.params.Ed25519KeyGenerationParameters
import org.testcontainers.shaded.org.bouncycastle.crypto.util.OpenSSHPrivateKeyUtil
import org.testcontainers.shaded.org.bouncycastle.crypto.util.OpenSSHPublicKeyUtil
import org.testcontainers.shaded.org.bouncycastle.util.io.pem.PemObject
import org.testcontainers.shaded.org.bouncycastle.util.io.pem.PemWriter


internal class FtpServiceTest : FunSpec({

    val keyPair = generateKeyPair()
    val privateKeyFile = createPrivateKeyFile(keyPair.private)
    val genericContainer = setupSftpTestContainer(keyPair.public)
    lateinit var ftpService: FtpService


    fun sftpConfig() = PropertiesConfig.SftpConfig(
        host = genericContainer.host,
        username = "foo",
        privateKey = privateKeyFile.absolutePath,
        privateKeyPassword = "pass",
        port = genericContainer.getMappedPort(22)
    )


    beforeSpec {
        genericContainer.start()
        val sftpSession = SftpConfig(sftpConfig()).createSftpConnection()
        ftpService = FtpService(sftpSession)
    }

    afterSpec {
        genericContainer.stop()
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

private fun setupSftpTestContainer(publicKey: AsymmetricKeyParameter): GenericContainer<*> {
    val publicKeyAsBytes = convertToByteArray(publicKey)
    return GenericContainer("atmoz/sftp:alpine")
        .withCopyToContainer(
            Transferable.of(publicKeyAsBytes),
            "/home/foo/.ssh/keys/id_rsa.pub"
        )
        .withExposedPorts(22)
        .withCommand("foo::::inbound,inbound/ferdig,outbound")
}

private fun createPrivateKeyFile(privateKey: AsymmetricKeyParameter): File {
    val privateKeyString = convertToString(privateKey)
    return File("src/test/resources/privateKey").apply {
        writeText(privateKeyString)
    }
}

private fun generateKeyPair(): AsymmetricCipherKeyPair {
    val keyPairGenerator = Ed25519KeyPairGenerator()
    keyPairGenerator.init(Ed25519KeyGenerationParameters(SecureRandom()))
    return keyPairGenerator.generateKeyPair()
}

private fun convertToString(privateKey: AsymmetricKeyParameter): String {
    val outputStream = ByteArrayOutputStream()
    PemWriter(OutputStreamWriter(outputStream)).use { writer ->
        val encodedPrivateKey =
            OpenSSHPrivateKeyUtil.encodePrivateKey(privateKey)
        writer.writeObject(
            PemObject(
                "OPENSSH PRIVATE KEY",
                encodedPrivateKey
            )
        )
    }
    return outputStream.toString()
}

private fun convertToByteArray(publicKey: AsymmetricKeyParameter): ByteArray {
    val openSshEncodedPublicKey = OpenSSHPublicKeyUtil.encodePublicKey(publicKey)
    val base64EncodedPublicKey = getEncoder().encodeToString(openSshEncodedPublicKey)
    return "ssh-ed25519 $base64EncodedPublicKey".toByteArray(StandardCharsets.UTF_8)
}