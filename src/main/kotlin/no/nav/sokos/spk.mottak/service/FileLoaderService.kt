package no.nav.sokos.spk.mottak.service

import java.sql.SQLException
import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.database.Db2DataSource
import no.nav.sokos.spk.mottak.database.FileInfo
import no.nav.sokos.spk.mottak.database.FileInfoRepository.insertFile
import no.nav.sokos.spk.mottak.database.FileInfoRepository.updateFileState
import no.nav.sokos.spk.mottak.database.InTransactionRepository.createInsertTransaction
import no.nav.sokos.spk.mottak.database.InTransactionRepository.deleteTransactions
import no.nav.sokos.spk.mottak.database.InTransactionRepository.insertTransaction
import no.nav.sokos.spk.mottak.database.LopenummerRepository.findMaxLopenummer
import no.nav.sokos.spk.mottak.database.LopenummerRepository.updateLopenummer
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.useAndHandleErrors
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.useConnectionWithRollback
import no.nav.sokos.spk.mottak.database.fileInfoFromStartRecord
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.modell.EndRecord
import no.nav.sokos.spk.mottak.modell.StartRecord
import no.nav.sokos.spk.mottak.modell.Transaction
import no.nav.sokos.spk.mottak.service.FileProducer.lagAvviksfil
import no.nav.sokos.spk.mottak.validator.ValidationFileStatus
import no.nav.sokos.spk.mottak.validator.ValidatorSpkFile

class FileLoaderService(
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
            val transactions: MutableList<Transaction> = mutableListOf()
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
                            val fileInfo = fileInfoFromStartRecord(startRecord, filename)
                            db2DataSource.connection.useConnectionWithRollback {
                                fileInfoId = it.insertFile(fileInfo)
                                // TODO: Legge inn EndretAv/EndretDato
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
                                if (totalRecord % batchsize == 0) {
                                    db2DataSource.connection.useAndHandleErrors {
                                        val ps = it.createInsertTransaction()
                                        transactions.forEach { transaction ->
                                            insertTransaction(ps, transaction, fileInfoId)
                                        }
                                        ps.executeBatch()
                                        it.commit()
                                        println("Batch executed $totalRecord records")
                                        transactions.clear()
                                    }
                                } else {
                                    transactions += transaction
                                }
                            } else {
                                println("End-record: '$record'")
                                endRecord = parseEndRecord(record)
                                println("Parset end-record: $endRecord")
                            }
                        }
                    }
                }
                val validator =
                    ValidatorSpkFile(startRecord, endRecord, totalRecord.minus(2), totalBelop, maxLopenummer)
                val validationFileStatus = validator.validateStartAndEndRecord()
                logger.info("ValidationFileStatus: $validationFileStatus")
                if (validationFileStatus != ValidationFileStatus.OK) {
                    db2DataSource.connection.useConnectionWithRollback {
                        // TODO: Legge inn feiltekst og EndretAv/EndretDato
                        it.updateFileState(FileState.AVV.name, "SPK", "ANV", fileInfoId)
                        it.deleteTransactions(fileInfoId)
                        it.commit()
                        exceptionHandled = true
                    }
                    lagAvviksfil(ftpService, startRecordUnparsed, validationFileStatus)
                } else {
                    db2DataSource.connection.useAndHandleErrors {
                        // TODO: Legge inn EndretAv/EndretDato
                        it.updateFileState(FileState.GOD.name, "SPK", "ANV", fileInfoId)
                        val ps = it.createInsertTransaction()
                        transactions.forEach { transaction ->
                            insertTransaction(ps, transaction, fileInfoId)
                        }
                        ps.executeBatch()
                        println("Batch executed $totalRecord records")
                        it.commit()
                    }
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
                if (!exceptionHandled) {
                    db2DataSource.connection.useConnectionWithRollback {
                        // TODO: Legge inn feiltekst og EndretAv/EndretDato
                        it.updateFileState(FileState.AVV.name, "SPK", "ANV", fileInfoId)
                        it.deleteTransactions(fileInfoId)
                        it.commit()
                    }
                }
                lagAvviksfil(ftpService, startRecordUnparsed, status)
            }
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
