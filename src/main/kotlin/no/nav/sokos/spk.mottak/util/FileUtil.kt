package no.nav.sokos.spk.mottak.util

import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.validator.FileStatusValidation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

object FileUtil {

    fun createAvviksFile(startRecord: String, fileStatusValidation: FileStatusValidation): File {
        val responseRecord = startRecord.replaceRange(76, 78, fileStatusValidation.code)
            .replaceRange(78, 113, fileStatusValidation.message)
        val fileName = "SPK_NAV_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}_ANV"
        try {
            val file = File(fileName)
            if (!file.createNewFile()) {
                logger.error("Anvisningsreturfil eksisterer allerede")
                throw RuntimeException("Anvisningsreturfil eksisterer allerede")
            }
            file.writeText(responseRecord)
            return file
        } catch (ex: Exception) {
            logger.error("Feil ved produksjon av anvisningsreturfil: ${ex.message}")
            throw ex
        }
    }
}