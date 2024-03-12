package no.nav.sokos.spk.mottak.service

import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.database.Db2DataSource
import no.nav.sokos.spk.mottak.database.FileInfo
import no.nav.sokos.spk.mottak.database.FileInfoRepository.findMaxLopenummer
import no.nav.sokos.spk.mottak.database.FileInfoRepository.insertFile
import no.nav.sokos.spk.mottak.database.FileInfoRepository.updateFileState
import no.nav.sokos.spk.mottak.database.InTransactionRepository.createInsertTransaction
import no.nav.sokos.spk.mottak.database.InTransactionRepository.deleteTransactions
import no.nav.sokos.spk.mottak.database.InTransactionRepository.insertTransaction
import no.nav.sokos.spk.mottak.database.LopenummerRepository.updateLopenummer
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.executeBatchConditional
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.executeBatchUnConditional
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.useAndHandleErrors
import no.nav.sokos.spk.mottak.database.fileInfoFromStartRecord
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.modell.EndRecord
import no.nav.sokos.spk.mottak.modell.StartRecord
import no.nav.sokos.spk.mottak.service.FileProducer.lagAvviksfil
import no.nav.sokos.spk.mottak.validator.ValidationFileStatus
import no.nav.sokos.spk.mottak.validator.ValidatorSpkFile
import java.io.BufferedReader
import java.io.FileReader
import java.math.BigDecimal
import java.sql.SQLException

class FileLoaderService(
    private val db2DataSource: Db2DataSource = Db2DataSource(),
    private val ftpService: FtpService = FtpService()
) {

    fun parseFiles() {
        ftpService.listAllFiles(FtpService.Directories.INBOUND.name).forEach { it ->
            var totalRecord = 0
            lateinit var startRecord: StartRecord
            lateinit var startRecordUnparsed: String
            lateinit var endRecord: EndRecord
            lateinit var validationFileStatus: ValidationFileStatus
            lateinit var fileInfo: FileInfo
            val reader = BufferedReader(FileReader(it))
            var maxLopenummer = 0
            var totalBelop = BigDecimal(0.0)
            var record: String
            var fileInfoId = 0
            var batchQuery = db2DataSource.connection.createInsertTransaction()
            try {
                while (reader.readLine().also { record = it } != null) {
                    when (totalRecord++) {
                        0 -> {
                            startRecordUnparsed = record
                            startRecord = parseStartRecord(record)
                            fileInfo = fileInfoFromStartRecord(startRecord, it)
                            db2DataSource.connection.useAndHandleErrors {
                                fileInfoId = it.insertFile(fileInfo)
                                it.updateLopenummer(fileInfoId, "SPK", "ANV")
                                it.commit()
                            }
                        }

                        else -> {
                            if (isTransactionRecord(record)) {
                                val transaction = parseTransaction(record)
                                totalBelop = totalBelop.add(BigDecimal(transaction.belopStr))
                                batchQuery = insertTransaction(batchQuery, transaction, fileInfoId).apply {
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
                    maxLopenummer = it.findMaxLopenummer("SPK", "ANV")
                }
                val validator = ValidatorSpkFile(startRecord, endRecord, totalRecord, totalBelop, maxLopenummer)
                validationFileStatus = validator.validateStartAndEndRecord()
                logger.info("ValidationFileStatus: $validationFileStatus")
                if (validationFileStatus != ValidationFileStatus.OK) {
                    db2DataSource.connection.useAndHandleErrors {
                        it.updateFileState(FileState.AVV.name, "SPK", "ANV")
                        it.deleteTransactions(fileInfoId)
                        it.commit()
                    }
                    lagAvviksfil(startRecordUnparsed, validationFileStatus)
                } else {
                    db2DataSource.connection.useAndHandleErrors {
                        it.updateFileState(FileState.GOD.name, "SPK", "ANV")
                    }
                    batchQuery.executeBatchUnConditional(db2DataSource.connection)
                }
            } catch (e: Exception) {
                var status: ValidationFileStatus
                if (e is ValidationException) {
                    logger.error("Valideringsfeil: ${e.message}")
                    status = mapToFault(e.statusCode)
                }
                else if (e is SQLException) {
                    logger.error("Feil ved databaseoperasjon: ${e.message}")
                    status = ValidationFileStatus.UKJENT
                }
                else {
                    logger.error("Feil ved innlesing av fil: ${e.message}")
                    status = ValidationFileStatus.UKJENT
                }
                db2DataSource.connection.useAndHandleErrors {
                    it.updateFileState(FileState.AVV.name, "SPK", "ANV")
                    it.deleteTransactions(fileInfoId)
                    it.commit()
                }
                lagAvviksfil(startRecordUnparsed, status)
            } finally {
                reader.close()
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