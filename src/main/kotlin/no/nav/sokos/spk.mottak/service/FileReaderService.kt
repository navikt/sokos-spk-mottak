package no.nav.sokos.spk.mottak.service

import java.sql.SQLException
import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.database.Db2DataSource
import no.nav.sokos.spk.mottak.database.FileInfoRepository.insertFile
import no.nav.sokos.spk.mottak.database.FileInfoRepository.updateFileState
import no.nav.sokos.spk.mottak.database.InTransactionRepository.createInsertTransaction
import no.nav.sokos.spk.mottak.database.InTransactionRepository.deleteTransactions
import no.nav.sokos.spk.mottak.database.InTransactionRepository.insertTransaction
import no.nav.sokos.spk.mottak.database.LopenummerRepository.findMaxLopenummer
import no.nav.sokos.spk.mottak.database.LopenummerRepository.updateLopenummer
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.useAndHandleErrors
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.useConnectionWithRollback
import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FileState
import no.nav.sokos.spk.mottak.domain.fileInfoFromStartRecord
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransactionRecord
import no.nav.sokos.spk.mottak.util.FileUtil.createAvviksFile
import no.nav.sokos.spk.mottak.validator.FileStatusValidation
import no.nav.sokos.spk.mottak.validator.FileValidation

class FileReaderService(
    private val db2DataSource: Db2DataSource = Db2DataSource(),
    private val ftpService: FtpService = FtpService()
) {
    private var batchsize: Int = 4000

    fun parseFiles() {
        ftpService.downloadFiles().forEach { (filename, content) ->
            println("Filnavn: '$filename'")
            println("Innhold: '$content'")
            var totalRecord = 0
            lateinit var startRecord: StartRecord
            lateinit var startRecordUnparsed: String
            lateinit var endRecord: EndRecord
            var exceptionHandled = false
            var maxLopenummer = 0
            var totalBelop: Long = 0
            var fileInfoId = 0
            val transactionRecords: MutableList<TransactionRecord> = mutableListOf()
            try {
                for (record in content) {
                    val fileParser = FileParser(record)
                    when (totalRecord++) {
                        0 -> {
                            println("Start-record: '$record'")
                            startRecordUnparsed = record
                            startRecord = fileParser.parseStartRecord()
                            db2DataSource.connection.useAndHandleErrors {
                                maxLopenummer = it.findMaxLopenummer(FILETYPE_ANVISER)
                            }
                            println("Parset start-record: $startRecord")
                            val fileInfo = fileInfoFromStartRecord(startRecord, filename)
                            db2DataSource.connection.useConnectionWithRollback {
                                fileInfoId = it.insertFile(fileInfo)
                                // TODO: Legge inn EndretAv/EndretDato
                                it.updateLopenummer(startRecord.filLopenummer, FILETYPE_ANVISER)
                                it.commit()
                            }
                        }

                        else -> {
                            if (content.size != totalRecord) {
                                println("Transaksjonsrecord: '$record'")
                                val transaction = fileParser.parseTransaction()
                                totalBelop += transaction.belopStr.toLong()
                                println("Parset transaksjon: $transaction")
                                if (totalRecord % batchsize == 0) {
                                    db2DataSource.connection.useAndHandleErrors {
                                        val ps = it.createInsertTransaction()
                                        transactionRecords.forEach { transaction ->
                                            insertTransaction(ps, transaction, fileInfoId)
                                        }
                                        ps.executeBatch()
                                        it.commit()
                                        println("Batch executed $totalRecord records")
                                        transactionRecords.clear()
                                    }
                                } else {
                                    transactionRecords += transaction
                                }
                            } else {
                                println("End-record: '$record'")
                                endRecord = fileParser.parseEndRecord()
                                println("Parset end-record: $endRecord")
                            }
                        }
                    }
                }
                val validator =
                    FileValidation(startRecord, endRecord, totalRecord.minus(2), totalBelop, maxLopenummer)
                val validationFileStatus = validator.validateStartAndEndRecord()
                logger.info("ValidationFileStatus: $validationFileStatus")
                if (validationFileStatus != FileStatusValidation.OK) {
                    db2DataSource.connection.useConnectionWithRollback {
                        // TODO: Legge inn feiltekst og EndretAv/EndretDato
                        it.updateFileState(FileState.AVV.name, FILETYPE_ANVISER, fileInfoId)
                        it.deleteTransactions(fileInfoId)
                        it.commit()
                        exceptionHandled = true
                    }
                    moveAvviksFile(startRecordUnparsed, validationFileStatus)
                } else {
                    db2DataSource.connection.useAndHandleErrors {
                        // TODO: Legge inn EndretAv/EndretDato
                        it.updateFileState(FileState.GOD.name, FILETYPE_ANVISER, fileInfoId)
                        val ps = it.createInsertTransaction()
                        transactionRecords.forEach { transaction ->
                            insertTransaction(ps, transaction, fileInfoId)
                        }
                        ps.executeBatch()
                        println("Batch executed $totalRecord records")
                        it.commit()
                    }
                }
            } catch (e: Exception) {
                val status: FileStatusValidation
                when (e) {
                    is ValidationException -> {
                        logger.error("Valideringsfeil: ${e.message}")
                        status = mapToFault(e.statusCode)
                    }

                    is SQLException -> {
                        logger.error("Feil ved databaseoperasjon: ${e.message}")
                        status = FileStatusValidation.UKJENT
                    }

                    else -> {
                        logger.error("Feil ved innlesing av fil: ${e.message}")
                        status = FileStatusValidation.UKJENT
                    }
                }
                if (!exceptionHandled) {
                    db2DataSource.connection.useConnectionWithRollback {
                        // TODO: Legge inn feiltekst og EndretAv/EndretDato
                        it.updateFileState(FileState.AVV.name, FILETYPE_ANVISER, fileInfoId)
                        it.deleteTransactions(fileInfoId)
                        it.commit()
                    }
                }
                moveAvviksFile(startRecordUnparsed, status)
            }
        }
    }

    private fun moveAvviksFile(startRecordUnparsed: String, status: FileStatusValidation) {
        val avviksFil = createAvviksFile(startRecordUnparsed, status)
        ftpService.moveFile(avviksFil.name, Directories.OUTBOUND, Directories.ANVISNINGSRETUR)
    }
}

private fun mapToFault(exceptionKode: String): FileStatusValidation {
    return when (exceptionKode) {
        "04" -> FileStatusValidation.UGYLDIG_FILLOPENUMMER
        "06" -> FileStatusValidation.UGYLDIG_RECTYPE
        "08" -> FileStatusValidation.UGYLDIG_SUMBELOP
        "09" -> FileStatusValidation.UGYLDIG_PRODDATO
        else -> FileStatusValidation.UKJENT
    }
}
