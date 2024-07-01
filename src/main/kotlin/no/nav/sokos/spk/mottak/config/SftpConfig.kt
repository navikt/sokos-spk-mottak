package no.nav.sokos.spk.mottak.config

import com.jcraft.jsch.JSch
import com.jcraft.jsch.Logger
import com.jcraft.jsch.Session
import java.util.Properties
import mu.KotlinLogging
import org.slf4j.LoggerFactory

private val logger = KotlinLogging.logger {}

class SftpConfig(
    private val sftpProperties: PropertiesConfig.SftpProperties = PropertiesConfig.SftpProperties(),
) {
    fun createSftpConnection(): Session {

        return JSch().apply {
            JSch.setLogger(JSchLogger())
            addIdentity(sftpProperties.privateKey, sftpProperties.privateKeyPassword)
        }.run {
            logger.debug { "Oppretter connection med privat nøkkel på host: ${sftpProperties.host}:${sftpProperties.port}" }
            getSession(sftpProperties.username, sftpProperties.host, sftpProperties.port)
        }.also {
            it.setConfig("StrictHostKeyChecking", "no")
            it.setConfig("PreferredAuthentications", "password")
            it.connect()
            logger.debug { "Åpner session på host: ${sftpProperties.host}:${sftpProperties.port}" }
        }
    }
}

class JSchLogger : Logger {
    private val logger = LoggerFactory.getLogger(JSch::class.java)

    override fun isEnabled(level: Int): Boolean {
        return level == Logger.DEBUG && logger.isDebugEnabled
    }

    override fun log(
        level: Int,
        message: String,
    ) {
        when (level) {
            Logger.DEBUG -> logger.debug(message)
            Logger.INFO -> logger.info(message)
            Logger.WARN -> logger.warn(message)
            Logger.ERROR -> logger.error(message)
            Logger.FATAL -> logger.error(message)
        }
    }
}
