package no.nav.sokos.spk.mottak.service

import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.validator.ValidationFileStatus
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileProducer {

    fun lagAvviksfil(ftpService:FtpService = FtpService(), startRecord: String, validationFileStatus: ValidationFileStatus) {
        val responseRecord = startRecord.replaceRange(76, 78, validationFileStatus.code)
            .replaceRange(78, 113, validationFileStatus.message)
        val fileName = "SPK_NAV_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}_ANV"
        try {
            val file = File(fileName)
            if (!file.createNewFile()) {
                logger.error("Anvisningsreturfil eksisterer allerede")
                throw RuntimeException("Anvisningsreturfil eksisterer allerede")
            }
            file.writeText(responseRecord)
            ftpService.moveFile(file.name, Directories.OUTBOUND, Directories.ANVISNINGSRETUR)
        } catch (ex: Exception) {
            logger.error("Feil ved produksjon av anvisningsreturfil: ${ex.message}")
            throw ex
        }
    }
}