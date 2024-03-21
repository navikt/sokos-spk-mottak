package no.nav.sokos.spk.mottak.service

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
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransactionRecord
import no.nav.sokos.spk.mottak.domain.record.toFileInfo
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.util.FileUtil.createAvviksRecord
import no.nav.sokos.spk.mottak.util.FileUtil.createFileName
import no.nav.sokos.spk.mottak.validator.FileStatusValidation
import no.nav.sokos.spk.mottak.validator.FileValidation.validateStartAndEndRecord

private const val BATCH_SIZE: Int = 10000

class FileReaderService(
    private val db2DataSource: Db2DataSource = Db2DataSource(),
    private val ftpService: FtpService = FtpService()
) {


    fun parseFiles() {
        ftpService.downloadFiles().forEach { (filename, content) ->
            println("Filnavn: '$filename'")

            val transactionRecords: MutableList<TransactionRecord> = mutableListOf()
            var fileInfoId = 0
            lateinit var recordData: RecordData
            try {
                recordData = readRecords(filename, content, transactionRecords)
                fileInfoId = recordData.startRecord.fileInfoId
                val validationFileStatus = validateStartAndEndRecord(recordData)
                logger.info("ValidationFileStatus: $validationFileStatus")
                if (validationFileStatus != FileStatusValidation.OK) {
                    db2DataSource.connection.useConnectionWithRollback {
                        // TODO: Legge inn feiltekst og EndretAv/EndretDato
                        it.updateFileState(FileState.AVV.name, FILETYPE_ANVISER, fileInfoId)
                        // TODO: Legge inn EndretAv/EndretDato
                        it.updateLopenummer(recordData.startRecord.filLopenummer, FILETYPE_ANVISER)
                    }
                    db2DataSource.connection.useAndHandleErrors {
                        it.deleteTransactions(fileInfoId)
                        it.commit()
                    }
                    createAvviksFil(recordData.startRecord.rawRecord, validationFileStatus)
                } else {
                    db2DataSource.connection.useConnectionWithRollback {
                        // TODO: Legge inn EndretAv/EndretDato
                        it.updateFileState(FileState.GOD.name, FILETYPE_ANVISER, fileInfoId)
                        // TODO: Legge inn EndretAv/EndretDato
                        it.updateLopenummer(recordData.startRecord.filLopenummer, FILETYPE_ANVISER)
                        val ps = it.createInsertTransaction()
                        transactionRecords.forEach { transaction ->
                            insertTransaction(ps, transaction, fileInfoId)
                        }
                        ps.executeBatch()
                        println("Batch executed ${recordData.numberOfRecord} records")
                    }
                }
            } catch (e: Exception) {
                val status: FileStatusValidation
                when (e) {
                    is ValidationException -> {
                        logger.error("Valideringsfeil: ${e.message}")
                        status = mapToFault(e.statusCode)
                    }
                    else -> {
                        logger.error("Ukjent feil ved innlesing av fil: ${e.message}")
                        status = FileStatusValidation.UKJENT
                    }
                }
                db2DataSource.connection.useConnectionWithRollback {
                    // TODO: Legge inn feiltekst og EndretAv/EndretDato
                    it.updateFileState(FileState.AVV.name, FILETYPE_ANVISER, fileInfoId)
                    // TODO: Legge inn EndretAv/EndretDato
                    it.updateLopenummer(recordData.startRecord.filLopenummer, FILETYPE_ANVISER)
                }
                db2DataSource.connection.useAndHandleErrors {
                    it.deleteTransactions(fileInfoId)
                    it.commit()
                }
                createAvviksFil(recordData.startRecord.rawRecord, status)
            }
        }
    }

    private fun readRecords(
        filename: String,
        content: List<String>,
        transactionRecords: MutableList<TransactionRecord>
    ): RecordData {
        var totalRecord = 0
        var totalBelop = 0L
        var maxLopenummer = 0
        lateinit var startRecord: StartRecord
        lateinit var endRecord: EndRecord
        db2DataSource.connection.useAndHandleErrors {
            maxLopenummer = it.findMaxLopenummer(FILETYPE_ANVISER)
        }
        println("Innhold størrelse: ${content.size}")
        content.forEach { record ->
            val fileParser = FileParser(record)
            if (totalRecord++ == 0) {
                startRecord = handleStartRecord(record, filename, fileParser)
            } else {
                if (content.size != totalRecord) {
                    val transaction = fileParser.parseTransaction()
                    totalBelop += transaction.belopStr.toLong()
                    handleTransactionRecord(transaction, startRecord, transactionRecords, totalRecord)
                } else {
                    println("End-record: '$record'")
                    endRecord = handleEndRecord(fileParser)
                }
            }
        }
        return RecordData(startRecord, endRecord, totalRecord, totalBelop, maxLopenummer)
    }

    private fun handleStartRecord(record: String, filename: String, fileParser: FileParser): StartRecord {
        println("Start-record: '$record'")
        val startRecord: StartRecord = fileParser.parseStartRecord().apply { rawRecord = record }

        println("Parset start-record: $startRecord")
        val fileInfo = startRecord.toFileInfo(filename)
        db2DataSource.connection.useConnectionWithRollback {
            startRecord.fileInfoId = it.insertFile(fileInfo)
        }
        return startRecord
    }

    private fun handleTransactionRecord(
        transaction: TransactionRecord,
        startRecord: StartRecord,
        transactionRecords: MutableList<TransactionRecord>,
        totalRecord: Int
    ) {
        if (totalRecord % BATCH_SIZE == 0) {
            db2DataSource.connection.useConnectionWithRollback {
                val ps = it.createInsertTransaction()
                transactionRecords.forEach { transaction ->
                    insertTransaction(ps, transaction, startRecord.fileInfoId)
                }
                ps.executeBatch()
                println("Batch executed $totalRecord records")
                transactionRecords.clear()
            }
        } else {
            transactionRecords += transaction
        }
    }

    private fun handleEndRecord(fileParser: FileParser): EndRecord {
        val endRecord = fileParser.parseEndRecord()
        println("Parset end-record: $endRecord")
        return endRecord
    }

    private fun createAvviksFil(startRecordUnparsed: String, status: FileStatusValidation) {
        val fileName = createFileName()
        val content = createAvviksRecord(startRecordUnparsed, status)
        ftpService.createFile(fileName, Directories.ANVISNINGSRETUR, content)
    }

    private fun mapToFault(exceptionKode: String): FileStatusValidation {
        return when (exceptionKode) {
            "04" -> FileStatusValidation.UGYLDIG_FILLOPENUMMER
            "06" -> FileStatusValidation.UGYLDIG_RECTYPE
            "09" -> FileStatusValidation.UGYLDIG_PRODDATO
            else -> FileStatusValidation.UKJENT
        }
    }
}
