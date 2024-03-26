package no.nav.sokos.spk.mottak.service

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import java.io.ByteArrayOutputStream
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SftpConfig

private val logger = KotlinLogging.logger {}

enum class Directories(var value: String) {
    INBOUND("/inbound"), FERDIG("/inbound/ferdig"), ANVISNINGSRETUR("/outbound/anvisningsretur")
}

class FtpService(
    private val sftpSession: Session = SftpConfig(PropertiesConfig.SftpConfig()).createSftpConnection(),
) {

    fun createFile(fileName: String, directory: Directories, content: String) {
        getSftpChannel().apply {
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
        getSftpChannel().apply {
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
        getSftpChannel().apply {
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

    private fun getSftpChannel(): ChannelSftp {
        val channelSftp = sftpSession.openChannel("sftp") as ChannelSftp
        return channelSftp.apply {
            connect()
        }
    }
}

