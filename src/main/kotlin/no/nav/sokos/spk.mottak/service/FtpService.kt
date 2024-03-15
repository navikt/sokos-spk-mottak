package no.nav.sokos.spk.mottak.service

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.Slf4jLogger
import java.io.ByteArrayOutputStream
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.logger

enum class Directories(var value: String) {
    INBOUND("/inbound"), OUTBOUND("/outbound"), ANVISNINGSRETUR("/outbound/anvisningsretur")
}

class FtpService(
    private val ftpConfig: PropertiesConfig.FtpConfig = PropertiesConfig.FtpConfig(), jsch: JSch = JSch()
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
        JSch.setLogger(Slf4jLogger())
        connect()
    }

    private fun connect() {
        try {
            session.connect()
            sftpChannel = session.openChannel("sftp") as ChannelSftp
            sftpChannel.connect()
        } catch (e: JSchException) {
            logger.error("Feil i FTP oppkobling: ${e.message}")

        }
    }

    fun downloadFiles(directory: Directories = Directories.INBOUND): Map<String, List<String>> =
        listFiles(directory.value).associateWith { sftpChannel.downloadFile("${directory.value}/$it") }

    private fun listFiles(directory: String): List<String> = sftpChannel.ls(directory).map { it.filename }.filter { it.contains(".txt") }

    private fun ChannelSftp.downloadFile(fileName: String): List<String> {
        val outputStream = ByteArrayOutputStream()
        try {
            get(fileName, outputStream)
        } catch (e: SftpException) {
            logger.error("Feil i henting av fil $fileName: ${e.message}")
        }

        return String(outputStream.toByteArray()).split("\r?\n|\r".toRegex()).filter { it.isNotEmpty() }
    }

    fun disconnect() {
        session.disconnect()
    }
}