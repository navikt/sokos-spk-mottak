package no.nav.sokos.spk.mottak.service

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.Session
import com.jcraft.jsch.SftpException
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.SftpConfig
import java.io.ByteArrayOutputStream

private val logger = KotlinLogging.logger {}

enum class Directories(var value: String) {
    INBOUND("/inbound"),
    FERDIG("/inbound/ferdig"),
    ANVISNINGSRETUR("/outbound/anvisningsretur"),
}

class FtpService(
    private val sftpSession: Session = SftpConfig(PropertiesConfig.SftpProperties()).createSftpConnection(),
) {
    fun createFile(
        fileName: String,
        directory: Directories,
        content: String,
    ) {
        getSftpChannel().apply {
            val path = "${directory.value}/$fileName"
            try {
                put(content.toByteArray().inputStream(), path)
                logger.debug { "$fileName ble opprettet i mappen $path" }
            } catch (e: SftpException) {
                logger.error { "$fileName ble ikke opprettet i mappen $path: ${e.message}" }
                throw e
            } finally {
                exit()
            }
        }
    }

    fun moveFile(
        fileName: String,
        from: Directories,
        to: Directories,
    ) {
        getSftpChannel().apply {
            val oldpath = "${from.value}/$fileName"
            val newpath = "${to.value}/$fileName"

            try {
                rename(oldpath, newpath)
                logger.debug { "$fileName ble flyttet fra mappen ${from.value} til mappen ${to.value}" }
            } catch (e: SftpException) {
                logger.error {
                    "$fileName ble ikke flyttet fra mappe $oldpath til mappe $newpath: ${e.message}"
                }
                throw e
            } finally {
                exit()
            }
        }
    }

    fun downloadFiles(directory: Directories = Directories.INBOUND): Map<String, List<String>> {
        var fileName = ""
        getSftpChannel().apply {
            try {
                return this.ls("${directory.value}/*")
                    .filter { !it.attrs.isDir }
                    .map { it.filename }
                    .sorted()
                    .associateWith {
                        fileName = "${directory.value}/$it"
                        val outputStream = ByteArrayOutputStream()
                        logger.debug { "$fileName ble lastet ned fra mappen $directory" }
                        get(fileName, outputStream)
                        String(outputStream.toByteArray()).split("\r?\n|\r".toRegex()).filter { file -> file.isNotEmpty() }
                    }
            } catch (e: SftpException) {
                logger.error { "$fileName ble ikke hentet. Feilmelding: ${e.message}" }
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
