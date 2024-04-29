package no.nav.sokos.spk.mottak.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import com.jcraft.jsch.Logger
import org.slf4j.LoggerFactory
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class SftpConfig(
    private val sftpConfig: PropertiesConfig.SftpConfig = PropertiesConfig.SftpConfig()
) {

    fun createSftpConnection(): Session {
        return JSch().apply {
            JSch.setLogger(JSchLogger())
            addIdentity(sftpConfig.privateKey, sftpConfig.privateKeyPassword)
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

class JSchLogger : Logger {
    private val logger = LoggerFactory.getLogger(JSch::class.java)

    override fun isEnabled(level: Int): Boolean {
        return level == Logger.DEBUG && logger.isDebugEnabled
    }

    override fun log(level: Int, message: String) {
        when (level) {
            Logger.DEBUG -> logger.debug(message)
            Logger.INFO -> logger.info(message)
            Logger.WARN -> logger.warn(message)
            Logger.ERROR -> logger.error(message)
            Logger.FATAL -> logger.error(message)
        }
    }
}
