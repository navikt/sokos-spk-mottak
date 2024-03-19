package no.nav.sokos.spk.mottak.util

import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.validator.FileStatusValidation
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

object FileUtil {

    fun createAvviksRecord(startRecord: String, fileStatusValidation: FileStatusValidation): String {
        return startRecord.replaceRange(76, 78, fileStatusValidation.code)
            .replaceRange(78, 113, fileStatusValidation.message)

    }

    fun createFileName(): String {
        return "SPK_NAV_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}_ANV"
    }
}