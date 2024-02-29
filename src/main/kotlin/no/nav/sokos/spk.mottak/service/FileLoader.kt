package no.nav.sokos.spk.mottak.service

import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.database.Db2DataSource
import no.nav.sokos.spk.mottak.database.FilInfo
import no.nav.sokos.spk.mottak.database.FilInfoRepository.insertFil
import no.nav.sokos.spk.mottak.database.FilInfoRepository.updateTilstand
import no.nav.sokos.spk.mottak.database.InnTransaksjonRepository.insertTransaksjon
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.useAndHandleErrors
import no.nav.sokos.spk.mottak.database.filInfoFraStartLinje
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.modell.FirstLine
import no.nav.sokos.spk.mottak.modell.LastLine
import no.nav.sokos.spk.mottak.validator.SpkFilformatFeil
import no.nav.sokos.spk.mottak.validator.ValidatorSpkFil
import java.io.BufferedReader
import java.io.FileReader
import java.math.BigDecimal

class FileLoaderService(
    private val db2DataSource: Db2DataSource = Db2DataSource(),
    private val config: PropertiesConfig.FileConfig = PropertiesConfig.FileConfig(),
    private val ftpService: FtpService
) {
    private val log = KotlinLogging.logger {}
    fun parseFiles() {
        ftpService.listAllFiles(config.innKatalog).forEach {
            var antallLinjer = 0
            lateinit var firstLine: FirstLine
            lateinit var lastLine: LastLine
            lateinit var filformatfeil: SpkFilformatFeil
            lateinit var filInfo: FilInfo
            var totalBelop = BigDecimal(0.0)
            var reader: BufferedReader
            var line: String
            try {
                val filnavn = it
                val file = FileReader(filnavn)
                reader = BufferedReader(file)
                while (reader.readLine().also { line = it } != null) {
                    antallLinjer++
                    when (antallLinjer) {
                        0 -> {
                            firstLine = parseFirsLine(line)
                            filInfo = filInfoFraStartLinje(firstLine, filnavn)
                            db2DataSource.connection.useAndHandleErrors {
                                it.insertFil(filInfo)
                            }
                        }
                        else -> {
                            if (erTransaksjon(line)) {
                                val transaksjon = parseTransactionLine(line)
                                totalBelop += transaksjon.belop
                                db2DataSource.connection.useAndHandleErrors {
                                    it.insertTransaksjon(transaksjon, filInfo.id)
                                }
                            } else if (erLastLine(line)) {
                                lastLine = parseLastLine(line)
                            } else {
                                throw ValidationException(kode = "06", message = "Transaksjonrecord er ikke type '02'")
                            }
                        }
                    }
                }
                val validator = ValidatorSpkFil(firstLine, lastLine, antallLinjer, totalBelop)
                filformatfeil = validator.validerLines()
                log.info("spkFilformatFeil: $filformatfeil")
                if (!filformatfeil.equals(SpkFilformatFeil.INGEN_FEIL)) {
                    db2DataSource.connection.useAndHandleErrors {
                        it.updateTilstand(FilTilstandType.AVV.name)
                    }
                    //lagAvviksfil(filformatfeil)
                } else {
                    db2DataSource.connection.useAndHandleErrors {
                        it.updateTilstand(FilTilstandType.GOD.name)
                    }
                    //commit()
                }
            } catch (e: ValidationException) {
                log.error("Valideringsfeil: ${e.message}")
                db2DataSource.connection.useAndHandleErrors {
                    it.updateTilstand(FilTilstandType.AVV.name)
                }
                //lagAvviksfil(mapToFeil(e.kode))
            }
        }
    }

    private fun mapToFeil(exceptionKode: String): SpkFilformatFeil {
        return when (exceptionKode) {
            "04" -> SpkFilformatFeil.FILLOPENUMMER
            "06" -> SpkFilformatFeil.RECORD_TYPE
            "09" -> SpkFilformatFeil.PRODUKSJONSDATO
            else -> SpkFilformatFeil.PARSING
        }
    }
}