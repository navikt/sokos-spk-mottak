package no.nav.sokos.spk.mottak.service

import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.database.Db2DataSource
import no.nav.sokos.spk.mottak.database.FileInfo
import no.nav.sokos.spk.mottak.database.FileInfoRepository.findMaxLopenummer
import no.nav.sokos.spk.mottak.database.FileInfoRepository.insertFile
import no.nav.sokos.spk.mottak.database.FileInfoRepository.updateFileState
import no.nav.sokos.spk.mottak.database.InTransactionRepository.insertTransaction
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.executeBatchConditional
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.executeBatchUnConditional
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.useAndHandleErrors
import no.nav.sokos.spk.mottak.database.fileInfoFromStartRecord
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.modell.EndRecord
import no.nav.sokos.spk.mottak.modell.StartRecord
import no.nav.sokos.spk.mottak.validator.ValidationFileStatus
import no.nav.sokos.spk.mottak.validator.ValidatorSpkFile
import java.io.BufferedReader
import java.io.FileReader
import java.math.BigDecimal
import java.sql.PreparedStatement

class FileLoaderService(
    private val db2DataSource: Db2DataSource = Db2DataSource(),
    private val ftpService: FtpService
) {

    fun parseFiles() {
        ftpService.listAllFiles(FtpService.Directories.INBOUND.name).forEach {
            var totalRecord = 0
            lateinit var startRecord: StartRecord
            lateinit var endRecord: EndRecord
            lateinit var validationFileStatus: ValidationFileStatus
            lateinit var fileInfo: FileInfo
            val fileName = it
            val reader = BufferedReader(FileReader(fileName))
            var maxLopenummer = 0
            var totalBelop = BigDecimal(0.0)
            var record: String
            var fileInfoId = 0
            lateinit var batchQuery: PreparedStatement
            try {
                while (reader.readLine().also { record = it } != null) {
                    when (totalRecord++) {
                        0 -> {
                            startRecord = parseStartRecord(record)
                            fileInfo = fileInfoFromStartRecord(startRecord, fileName)
                            db2DataSource.connection.useAndHandleErrors {
                                fileInfoId = it.insertFile(fileInfo)
                            }
                        }

                        else -> {
                            if (isTransactionRecord(record)) {
                                val transaction = parseTransaction(record)
                                totalBelop == totalBelop + BigDecimal(transaction.belopStr)
                                batchQuery = db2DataSource.connection.insertTransaction(transaction, fileInfoId).apply {
                                    executeBatchConditional(db2DataSource.connection)
                                }
                            } else if (isEndRecord(record)) {
                                endRecord = parseEndRecord(record)
                            } else {
                                throw ValidationException(
                                    ValidationFileStatus.UGYLDIG_RECTYPE.code,
                                    ValidationFileStatus.UGYLDIG_RECTYPE.message
                                )
                            }
                        }
                    }
                }
                db2DataSource.connection.useAndHandleErrors {
                    maxLopenummer = it.findMaxLopenummer("SPK")
                }
                val validator = ValidatorSpkFile(startRecord, endRecord, totalRecord, totalBelop, maxLopenummer)
                validationFileStatus = validator.validateStartAndEndRecord()
                logger.info("ValidationFileStatus: $validationFileStatus")
                if (validationFileStatus != ValidationFileStatus.OK) {
                    db2DataSource.connection.useAndHandleErrors {
                        it.updateFileState(FileState.AVV.name)
                    }
                    //lagAvviksfil(validationStatus)
                } else {
                    db2DataSource.connection.useAndHandleErrors {
                        it.updateFileState(FileState.GOD.name)
                    }
                    batchQuery.executeBatchUnConditional(db2DataSource.connection)
                }
            } catch (e: ValidationException) {
                logger.error("Valideringsfeil: ${e.message}")
                db2DataSource.connection.useAndHandleErrors {
                    it.updateFileState(FileState.AVV.name)
                }
                //lagAvviksfil(mapToFault(e.statusCode))
            }
        }
    }

    private fun mapToFault(exceptionKode: String): ValidationFileStatus {
        return when (exceptionKode) {
            "04" -> ValidationFileStatus.UGYLDIG_FILLOPENUMMER
            "06" -> ValidationFileStatus.UGYLDIG_RECTYPE
            "08" -> ValidationFileStatus.UGYLDIG_SUMBELOP
            "09" -> ValidationFileStatus.UGYLDIG_PRODDATO
            else -> ValidationFileStatus.UKJENT
        }
    }
}