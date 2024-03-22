package no.nav.sokos.spk.mottak.service

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSchException
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.Slf4jLogger
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import java.io.ByteArrayOutputStream

private val logger = KotlinLogging.logger {}

enum class Directories(var value: String) {
    INBOUND("/inbound"), OUTBOUND("/outbound"), FERDIG("/inbound/ferdig"), ANVISNINGSRETUR("/outbound/anvisningsretur")
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

    fun createFile(fileName: String, directory: Directories, content: String) =
        sftpChannel.createFile(fileName, directory, content)


    fun moveFile(fileName: String, from: Directories, to: Directories) {
        sftpChannel.moveFile(fileName, from, to)
    }

    fun downloadFiles(directory: Directories = Directories.INBOUND): Map<String, List<String>> =
        listFiles(directory.value).associateWith { sftpChannel.downloadFile("${directory.value}/$it") }

    private fun ChannelSftp.createFile(fileName: String, directory: Directories, content: String) {
        val path = "${directory.value}/$fileName"
        try {
            put(content.toByteArray().inputStream(), path)
        } catch (e: SftpException) {
            logger.error("Feil i opprettelse av fil $path: ${e.message}")
        }
    }

    private fun ChannelSftp.moveFile(fileName: String, from: Directories, to: Directories) {
        val oldpath = "${from.value}/$fileName"
        val newpath = "${to.value}/$fileName"

        try {
            rename(oldpath, newpath)
        } catch (e: SftpException) {
            logger.error("Feil i flytting av fil fra $oldpath til $newpath: ${e.message}")
        }
    }

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

}