package no.nav.sokos.spk.mottak.service

import com.jcraft.jsch.SftpException
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.SftpConfig
import java.io.ByteArrayOutputStream

private val logger = KotlinLogging.logger {}

enum class Directories(
    var value: String,
) {
    INBOUND("/inbound"),
    FERDIG("/inbound/ferdig"),
    ANVISNINGSRETUR("/outbound/anvisningsretur"),
}

class FtpService(
    private val sftpConfig: SftpConfig = SftpConfig(),
) {
    fun createFile(
        fileName: String,
        directory: Directories,
        content: String,
    ) {
        sftpConfig.channel { connector ->
            val path = "${directory.value}/$fileName"
            runCatching {
                connector.put(content.toByteArray().inputStream(), path)
                logger.debug { "$fileName ble opprettet i mappen $path" }
            }.onFailure { exception ->
                logger.error { "$fileName ble ikke opprettet i mappen $path: ${exception.message}" }
                throw exception
            }
        }
    }

    fun moveFile(
        fileName: String,
        from: Directories,
        to: Directories,
    ) {
        sftpConfig.channel { connector ->
            val oldpath = "${from.value}/$fileName"
            val newpath = "${to.value}/$fileName"

            runCatching {
                connector.rename(oldpath, newpath)
                logger.debug { "$fileName ble flyttet fra mappen ${from.value} til mappen ${to.value}" }
            }.onFailure { exception ->
                logger.error { "$fileName ble ikke flyttet fra mappe $oldpath til mappe $newpath: ${exception.message}" }
                throw exception
            }
        }
    }

    fun downloadFiles(directory: Directories = Directories.INBOUND): Map<String, List<String>> {
        var fileName = ""
        return sftpConfig.channel { connector ->
            try {
                connector
                    .ls("${directory.value}/*")
                    .filter { !it.attrs.isDir }
                    .map { it.filename }
                    .sorted()
                    .associateWith {
                        fileName = "${directory.value}/$it"
                        val outputStream = ByteArrayOutputStream()
                        logger.debug { "$fileName ble lastet ned fra mappen $directory" }
                        connector.get(fileName, outputStream)
                        String(outputStream.toByteArray()).split("\r?\n|\r".toRegex()).filter { file -> file.isNotEmpty() }
                    }
            } catch (e: SftpException) {
                logger.error { "$fileName ble ikke hentet. Feilmelding: ${e.message}" }
                throw e
            }
        }
    }
}
