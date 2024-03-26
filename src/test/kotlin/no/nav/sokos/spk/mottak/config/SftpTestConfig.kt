package no.nav.sokos.spk.mottak.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.Slf4jLogger
import org.testcontainers.containers.GenericContainer
import org.testcontainers.utility.MountableFile

const val INBOUND = "inbound"
const val OUTBOUND = "outbound"

class SftpTestConfig(
    private val sftpTestConfig: PropertiesConfig.SftpConfig = PropertiesConfig.SftpConfig(
        host = "localhost",
        username = "foo",
        privateKeyPassword = "pass",
        port = 22
    )
) {
    fun createSftpConnection(): Session {
        return JSch().apply {
            JSch.setLogger(Slf4jLogger())
        }.run {
            getSession(sftpTestConfig.username, sftpTestConfig.host, sftpTestConfig.port)
        }.also {
            it.setConfig("StrictHostKeyChecking", "no")
            it.setPassword(sftpTestConfig.privateKeyPassword)
            it.connect()
        }
    }

    fun getSftpTestContainer(): GenericContainer<*> {
        return GenericContainer("atmoz/sftp:alpine")
            .withCopyFileToContainer(
                MountableFile.forClasspathResource("$INBOUND/"),
                "/home/foo/test/$INBOUND"
            )
            .withExposedPorts(22)
            .withCommand("foo:pass:::$INBOUND")
    }

}