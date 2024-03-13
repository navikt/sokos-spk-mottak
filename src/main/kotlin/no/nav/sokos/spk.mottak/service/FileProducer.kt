package no.nav.sokos.spk.mottak.service

import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.validator.ValidationFileStatus
import java.io.File

object FileProducer {

    fun lagAvviksfil(startRecord: String, validationFileStatus: ValidationFileStatus) {
        val responseRecord = startRecord.replaceRange(76, 78, validationFileStatus.code)
            .replaceRange(78, 113, validationFileStatus.message)
        val lopenummer = startRecord.substring(24, 30)
        val fileName =
            "${Directories.ANVISNINGSRETUR}/SPK_NAV_${lopenummer}_INL"
        try {
            val file = File(fileName)
            if (!file.createNewFile()) {
                logger.error("Anvisningsreturfil eksisterer allerede")
                throw RuntimeException("Anvisningsreturfil eksisterer allerede")
            }
            file.writeText(responseRecord)
        } catch (ex: Exception) {
            logger.error("Feil ved produksjon av anvisningsreturfil: ${ex.message}")
            throw ex
        }
    }
}