package no.nav.sokos.spk.mottak.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.Slf4jLogger
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class SftpConfig(
    private val sftpConfig: PropertiesConfig.SftpConfig = PropertiesConfig.SftpConfig()
) {

    fun createSftpConnection(): Session {
        return JSch().apply {
            JSch.setLogger(Slf4jLogger())
            addIdentity(sftpConfig.privKey, sftpConfig.keyPass)
        }.run {
            logger.debug { "Oppretter connection med privat nøkkel på host: ${sftpConfig.host}:${sftpConfig.port}" }
            getSession(sftpConfig.username, sftpConfig.host, sftpConfig.port)
        }.also {
            it.setConfig("StrictHostKeyChecking", "no")
            it.connect()
            logger.debug { "Åpner session på host: ${sftpConfig.host}:${sftpConfig.port}" }
        }
    }
}