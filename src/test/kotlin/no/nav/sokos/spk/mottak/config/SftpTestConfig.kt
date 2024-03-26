package no.nav.sokos.spk.mottak.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.Slf4jLogger

class SftpTestConfig(
    private val sftpConfig: PropertiesConfig.SftpConfig = PropertiesConfig.SftpConfig()
) {
    fun createSftpConnection(): Session {
        return JSch().apply {
            JSch.setLogger(Slf4jLogger())
            addIdentity(sftpConfig.privKey, sftpConfig.keyPass)
        }.run {
            getSession(sftpConfig.username, sftpConfig.host, sftpConfig.port)
        }.also {
            it.setConfig("StrictHostKeyChecking", "no")
            it.connect()
        }
    }

}