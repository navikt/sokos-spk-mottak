package no.nav.sokos.spk.mottak.service

import java.math.BigDecimal
import java.sql.SQLException
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
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.useConnectionWithRollback
import no.nav.sokos.spk.mottak.database.fileInfoFromStartRecord
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.modell.EndRecord
import no.nav.sokos.spk.mottak.modell.StartRecord
import no.nav.sokos.spk.mottak.service.FileProducer.lagAvviksfil
import no.nav.sokos.spk.mottak.validator.ValidationFileStatus
import no.nav.sokos.spk.mottak.validator.ValidatorSpkFile

class FileLoaderService(
    private val db2DataSource: Db2DataSource = Db2DataSource(),
    private val ftpService: FtpService = FtpService()
) {

    fun parseFiles() {
        val test = ftpService.listFiles(Directories.INBOUND.value)
        println("HVA FÅR JEG UT? $test")
        ftpService.downloadFiles().forEach { (filename, content) ->
            println("Filnavn: '$filename'")
            println("Innhold: '$content'")
            var totalRecord = 0
            lateinit var startRecord: StartRecord
            lateinit var startRecordUnparsed: String
            lateinit var endRecord: EndRecord
            lateinit var validationFileStatus: ValidationFileStatus
            lateinit var fileInfo: FileInfo
            var maxLopenummer = 0
            var totalBelop: Long = 0
            var fileInfoId = 0
            var batchQuery = db2DataSource.connection.createInsertTransaction()
            try {
                for (record in content) {
                    when (totalRecord++) {
                        0 -> {
                            println("Start-record: '$record'")
                            startRecordUnparsed = record
                            startRecord = parseStartRecord(record)
                            db2DataSource.connection.useAndHandleErrors {
                                maxLopenummer = it.findMaxLopenummer("SPK", "ANV")
                            }
                            println("Parset start-record: $startRecord")
                            fileInfo = fileInfoFromStartRecord(startRecord, filename)
                            db2DataSource.connection.useConnectionWithRollback {
                                fileInfoId = it.insertFile(fileInfo)
                                it.updateLopenummer(startRecord.filLopenummer, "SPK", "ANV")
                                it.commit()
                            }
                        }

                        else -> {
                            if (content.size != totalRecord) {
                                println("Transaksjonsrecord: '$record'")
                                val transaction = parseTransaction(record)
                                totalBelop += transaction.belopStr.toLong()
                                println("Parset transaksjon: $transaction")
                                batchQuery.insertTransaction(transaction, fileInfoId)
                                batchQuery.executeBatchConditional(db2DataSource.connection)

                            } else {
                                println("End-record: '$record'")
                                endRecord = parseEndRecord(record)
                                println("Parset end-record: $endRecord")
                            }
                        }
                    }
                }
                val validator = ValidatorSpkFile(startRecord, endRecord, totalRecord.minus(2), totalBelop, maxLopenummer)
                validationFileStatus = validator.validateStartAndEndRecord()
                logger.info("ValidationFileStatus: $validationFileStatus")
                if (validationFileStatus != ValidationFileStatus.OK) {
                    db2DataSource.connection.useAndHandleErrors {
                        it.updateFileState(FileState.AVV.name, "SPK", "ANV", startRecord.filLopenummer)
                        it.commit()
                        it.deleteTransactions(fileInfoId)
                        it.commit()
                    }
//                    lagAvviksfil(startRecordUnparsed, validationFileStatus)
                } else {
                    db2DataSource.connection.useAndHandleErrors {
                        it.updateFileState(FileState.GOD.name, "SPK", "ANV", startRecord.filLopenummer)
                        it.commit()
                    }
                    batchQuery.executeBatchUnConditional(db2DataSource.connection)
                    db2DataSource.connection.commit()
                }
            } catch (e: Exception) {
                val status: ValidationFileStatus
                when (e) {
                    is ValidationException -> {
                        logger.error("Valideringsfeil: ${e.message}")
                        status = mapToFault(e.statusCode)
                    }

                    is SQLException -> {
                        logger.error("Feil ved databaseoperasjon: ${e.message}")
                        status = ValidationFileStatus.UKJENT
                    }

                    else -> {
                        logger.error("Feil ved innlesing av fil: ${e.message}")
                        status = ValidationFileStatus.UKJENT
                    }
                }
                db2DataSource.connection.useAndHandleErrors {
                    it.updateFileState(FileState.AVV.name, "SPK", "ANV", startRecord.filLopenummer)
                    it.deleteTransactions(fileInfoId)
                    it.commit()
                }
                lagAvviksfil(startRecordUnparsed, status)
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