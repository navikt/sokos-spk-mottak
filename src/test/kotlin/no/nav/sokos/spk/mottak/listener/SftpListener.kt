package no.nav.sokos.spk.mottak.listener

import com.github.dockerjava.api.model.ExposedPort
import com.github.dockerjava.api.model.PortBinding
import com.github.dockerjava.api.model.Ports
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.service.Directories.ANVISNINGSRETUR
import no.nav.sokos.spk.mottak.service.Directories.FERDIG
import no.nav.sokos.spk.mottak.service.Directories.INBOUND
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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets
import java.security.SecureRandom
import java.util.Base64

private val logger = KotlinLogging.logger {}

object SftpListener : TestListener {
    private val keyPair = generateKeyPair()
    private val privateKeyFile = createPrivateKeyFile(keyPair.private)
    private val genericContainer = setupSftpTestContainer(keyPair.public)

    val sftpProperties =
        PropertiesConfig.SftpProperties(
            host = "localhost",
            username = "foo",
            privateKey = privateKeyFile.absolutePath,
            privateKeyPassword = "pass",
            port = 5678,
        )

    override suspend fun beforeSpec(spec: Spec) {
        genericContainer.start()
    }

    override suspend fun afterSpec(spec: Spec) {
        genericContainer.stop()
    }

    private fun setupSftpTestContainer(publicKey: AsymmetricKeyParameter): GenericContainer<*> {
        val publicKeyAsBytes = convertToByteArray(publicKey)
        return GenericContainer("atmoz/sftp:alpine")
            .withCopyToContainer(
                Transferable.of(publicKeyAsBytes),
                "/home/foo/.ssh/keys/id_rsa.pub",
            ).withExposedPorts(22)
            .withCreateContainerCmdModifier { cmd -> cmd.hostConfig!!.withPortBindings(PortBinding(Ports.Binding.bindPort(5678), ExposedPort(22))) }
            .withCommand("foo::::${INBOUND.value},${FERDIG.value},outbound,${ANVISNINGSRETUR.value}")
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
                    encodedPrivateKey,
                ),
            )
        }
        return outputStream.toString()
    }

    private fun convertToByteArray(publicKey: AsymmetricKeyParameter): ByteArray {
        val openSshEncodedPublicKey = OpenSSHPublicKeyUtil.encodePublicKey(publicKey)
        val base64EncodedPublicKey = Base64.getEncoder().encodeToString(openSshEncodedPublicKey)
        return "ssh-ed25519 $base64EncodedPublicKey".toByteArray(StandardCharsets.UTF_8)
    }

    fun deleteFile(vararg fileName: String) {
        val deleteFilename = fileName.joinToString(separator = " ")
        val sftpConfig = SftpConfig(SftpListener.sftpProperties)

        sftpConfig.channel { connector ->
            runCatching {
                logger.info { "Fjerner fil: $deleteFilename" }
                fileName.forEach { connector.rm(it) }
            }.onFailure { exception ->
                logger.error { "Feil i fjerning av filer $deleteFilename: ${exception.message}" }
                throw exception
            }
        }
    }
}
