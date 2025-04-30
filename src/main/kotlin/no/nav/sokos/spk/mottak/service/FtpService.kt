package no.nav.sokos.spk.mottak.service

import java.io.ByteArrayOutputStream

import com.jcraft.jsch.SftpException
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.SftpConfig
import no.nav.sokos.spk.mottak.exception.SFtpException

private val logger = KotlinLogging.logger {}

enum class Directories(
    var value: String,
) {
    INBOUND("/inbound/anvisningsfil"),
    ANVISNINGSFIL_BEHANDLET("/inbound/anvisningsfilbehandlet"),
    ANVISNINGSRETUR("/outbound/anvisningsretur"),
    AVREGNINGSRETUR("/outbound/avregning"),
    AVREGNINGSRETUR_BEHANDLET("/outbound/avregningsfilbehandlet"),
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
                logger.error { "$fileName ble ikke opprettet i mappen $path. Feilmelding: ${exception.message}" }
                throw SFtpException("SFtp-feil: $exception")
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
                logger.error { "$fileName ble ikke flyttet fra mappe ${from.value}  til mappe ${to.value}. Feilmelding: ${exception.message}" }
                throw SFtpException("SFtp-feil: $exception")
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
                    .sortedWith(compareBy { it.split(".").getOrNull(5) ?: "" })
                    .associateWith {
                        fileName = "${directory.value}/$it"
                        val outputStream = ByteArrayOutputStream()
                        logger.debug { "$fileName ble lastet ned fra mappen $directory" }
                        connector.get(fileName, outputStream)
                        String(outputStream.toByteArray()).split("\r?\n|\r".toRegex()).filter { file -> file.isNotEmpty() }
                    }
            } catch (exception: SftpException) {
                logger.error { "$fileName ble ikke hentet. Feilmelding: ${exception.message}" }
                throw SFtpException("SFtp-feil: $exception")
            }
        }
    }
}
