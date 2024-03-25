package no.nav.sokos.spk.mottak.service

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.JSch.setLogger
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import com.jcraft.jsch.Slf4jLogger
import java.io.ByteArrayOutputStream
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig

private val logger = KotlinLogging.logger {}

enum class Directories(var value: String) {
    INBOUND("/inbound"), FERDIG("/inbound/ferdig"), ANVISNINGSRETUR("/outbound/anvisningsretur")
}

class FtpService(
    private val ftpConfig: PropertiesConfig.FtpConfig = PropertiesConfig.FtpConfig()
) {

    fun createFile(fileName: String, directory: Directories, content: String) {
        sftpChannel().apply {
            logger.debug { "Lager fil: $fileName i mappen: ${directory.value}" }
            val path = "${directory.value}/$fileName"
            try {
                put(content.toByteArray().inputStream(), path)
            } catch (e: SftpException) {
                logger.error { "Feil i opprettelse av fil $path: ${e.message}" }
                throw e
            } finally {
                exit()
            }
        }
    }


    fun moveFile(fileName: String, from: Directories, to: Directories) {
        sftpChannel().apply {
            logger.debug { "Flytter fil: $fileName fra ${from.value} til ${to.value}" }
            val oldpath = "${from.value}/$fileName"
            val newpath = "${to.value}/$fileName"

            try {
                rename(oldpath, newpath)
            } catch (e: SftpException) {
                logger.error {
                    "Feil i flytting av fil fra $oldpath til $newpath: ${e.message}"
                }
                throw e
            }
        }
    }

    fun downloadFiles(directory: Directories = Directories.INBOUND): Map<String, List<String>> {
        var fileName = ""
        sftpChannel().apply {
            try {
                return this.ls("${directory.value}/*")
                    .filter { !it.filename.contains("ferdig") }
                    .map { it.filename }
                    .sorted()
                    .associateWith {
                        fileName = "${directory.value}/$it"
                        logger.debug { "Henter fil: $fileName" }
                        val outputStream = ByteArrayOutputStream()

                        get(fileName, outputStream)
                        String(outputStream.toByteArray()).split("\r?\n|\r".toRegex()).filter { it.isNotEmpty() }
                    }
            } catch (e: SftpException) {
                logger.error { "Feil i henting av fil $fileName: ${e.message}" }
                throw e
            } finally {
                exit()
            }

        }
    }

    private fun openConnection(): Session {
        return JSch().apply {
            setLogger(Slf4jLogger())
            addIdentity(ftpConfig.privKey, ftpConfig.keyPass)
            setKnownHosts(ftpConfig.hostKey)
        }.run {
            logger.debug { "Oppretter connection med privat nøkkel på host: ${ftpConfig.server}:${ftpConfig.port}" }
            getSession(ftpConfig.username, ftpConfig.server, ftpConfig.port)
        }.also {
            it.setConfig("PreferredAuthentications", "publickey")
            it.connect()
            logger.debug { "Åpner session på host: ${ftpConfig.server}:${ftpConfig.port}" }
        }
    }


    private fun sftpChannel(): ChannelSftp {
        val channelSftp = openConnection().openChannel("sftp") as ChannelSftp
        return channelSftp.apply {
            connect()
            logger.debug { "Koblet SftpChannel på: ${ftpConfig.server}:${ftpConfig.port}" }
        }
    }
}

