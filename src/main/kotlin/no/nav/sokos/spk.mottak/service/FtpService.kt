package no.nav.sokos.spk.mottak.service

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.logger

class FtpService(
    private val ftpConfig: PropertiesConfig.FtpConfig = PropertiesConfig.FtpConfig(),
    jsch: JSch = JSch()
) {

    private val secureChannel: JSch = jsch.apply {
        addIdentity(ftpConfig.privKey, ftpConfig.keyPass)
        setKnownHosts(ftpConfig.hostKey)
    }

    private val session = secureChannel.getSession(ftpConfig.username, ftpConfig.server, ftpConfig.port).apply {
        setConfig("PreferredAuthentications", "publickey")
    }

    private lateinit var sftpChannel: ChannelSftp

    init {
        connect()
    }

    private fun connect() {
        try {
            session.connect()
            sftpChannel = session.openChannel("sftp") as ChannelSftp
            sftpChannel.connect()
        } catch (e: JSchException) {
            println(e.stackTrace)
            logger.error("Feil i FTP oppkobling: ${e.message}")

        }
    }

    fun listAllFiles(directory: String): List<String> = sftpChannel.ls(directory).map { it.filename }

}