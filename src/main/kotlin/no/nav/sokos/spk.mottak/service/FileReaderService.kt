package no.nav.sokos.spk.mottak.service

import kotliquery.TransactionalSession
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.database.FileInfoRepository
import no.nav.sokos.spk.mottak.database.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.database.LopenummerRepository
import no.nav.sokos.spk.mottak.database.config.TransactionalManager
import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FilTilstandType
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.toFileInfo
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.util.FileUtil.createAvviksRecord
import no.nav.sokos.spk.mottak.util.FileUtil.createFileName
import no.nav.sokos.spk.mottak.validator.FileStatusValidation
import no.nav.sokos.spk.mottak.validator.FileValidation.validateStartAndEndRecord

private const val BATCH_SIZE: Int = 20000
private val logger = KotlinLogging.logger {}

class FileReaderService(
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(),
    private val lopenummerRepository: LopenummerRepository = LopenummerRepository(),
    private val fileInfoRepository: FileInfoRepository = FileInfoRepository(),
    private val ftpService: FtpService = FtpService()
) {
    fun readAndParseFile() {

        val downloadFiles = ftpService.downloadFiles()
        downloadFiles.isNotEmpty().apply {
            logger.info { "Start innlesning av antall filer: ${downloadFiles.size}" }
        }

        downloadFiles.forEach { (filename, content) ->
            lateinit var recordData: RecordData
            runCatching {
                TransactionalManager.transaction { session ->
                    val maxLopenummer = lopenummerRepository.findMaxLopenummer(FILETYPE_ANVISER)

                    logger.info("Leser inn fil: '$filename'")
                    recordData = readRecords(content).apply {
                        this.maxLopenummer = maxLopenummer
                        this.filename = filename
                    }

                    // Validate recordData
                    val fileStatusValidation = validateStartAndEndRecord(recordData)
                    logger.debug("ValidationFileStatus: $fileStatusValidation")

                    when (fileStatusValidation) {
                        FileStatusValidation.OK -> saveRecordDataAndMoveFile(recordData, session)
                        else -> updateFileStatusAndUploadAvviksFil(recordData, fileStatusValidation, session)
                    }
                }
            }.onFailure { exception ->
                when {
                    exception is ValidationException ->
                        TransactionalManager.transaction { session ->
                            updateFileStatusAndUploadAvviksFil(recordData, mapToFault(exception.statusCode), session)
                        }

                    else -> throw RuntimeException("Unknown exception", exception)
                }
            }
        }
    }

    private fun saveRecordDataAndMoveFile(recordData: RecordData, session: TransactionalSession) {
        lopenummerRepository.updateLopenummer(recordData.startRecord.filLopenummer, FILETYPE_ANVISER, session)

        val filInfo = recordData.startRecord.toFileInfo(recordData.filename!!)
        val filInfoId = fileInfoRepository.insertFilInfo(filInfo, session)!!

        var antallInnTransaksjon: Int = 0;
        recordData.innTransaksjonList.chunked(BATCH_SIZE).forEach { innTransaksjonList ->
            antallInnTransaksjon += innTransaksjonList.size
            innTransaksjonRepository.insertTransactionBatch(innTransaksjonList, filInfoId, session)
            logger.debug("Antall innTransaksjon som er lagret i DB: $antallInnTransaksjon")
        }
        recordData.innTransaksjonList.clear()
        fileInfoRepository.updateFilInfoTilstandType(recordData.startRecord.fileInfoId, FilTilstandType.GOD.name, FILETYPE_ANVISER, session)
        ftpService.moveFile(recordData.filename!!, Directories.INBOUND, Directories.FERDIG)
    }

    private fun updateFileStatusAndUploadAvviksFil(recordData: RecordData, status: FileStatusValidation, session: TransactionalSession) {
        // Trenger å oppdatere lopenummer dersom det er en valideringsfeil.
        lopenummerRepository.updateLopenummer(recordData.startRecord.filLopenummer, FILETYPE_ANVISER, session)
        fileInfoRepository.updateFilInfoTilstandType(recordData.startRecord.fileInfoId, FilTilstandType.AVV.name, FILETYPE_ANVISER, session)
        createAvviksFil(recordData.startRecord.rawRecord, status)
        ftpService.moveFile(recordData.filename!!, Directories.INBOUND, Directories.FERDIG)
    }

    private fun readRecords(content: List<String>): RecordData {
        var totalRecord = 0
        var totalBelop = 0L
        lateinit var startRecord: StartRecord
        lateinit var endRecord: EndRecord
        val innTransaksjons: MutableList<InnTransaksjon> = mutableListOf()

        logger.debug("Innhold størrelse: ${content.size}")
        content.forEach { record ->
            if (totalRecord++ == 0) {
                startRecord = FileParser.parseStartRecord(record).apply { rawRecord = record }
                logger.debug("Start-record: $record")
            } else {
                if (content.size != totalRecord) {
                    val transaction = FileParser.parseTransaction(record)
                    totalBelop += transaction.belopStr.toLong()
                    innTransaksjons.add(transaction)
                } else {
                    logger.debug("Totalbelop: $totalBelop")
                    logger.debug("End-record: '$record'")
                    endRecord = FileParser.parseEndRecord(record)
                }
            }
        }
        return RecordData(
            startRecord = startRecord,
            endRecord = endRecord,
            innTransaksjonList = innTransaksjons,
            totalBelop = totalBelop
        )
    }

    private fun createAvviksFil(startRecordUnparsed: String, status: FileStatusValidation) {
        val fileName = createFileName()
        val content = createAvviksRecord(startRecordUnparsed, status)

        ftpService.createFile(fileName, Directories.ANVISNINGSRETUR, content)
    }
}

private fun mapToFault(exceptionKode: String): FileStatusValidation {
    return when (exceptionKode) {
        "04" -> FileStatusValidation.UGYLDIG_FILLOPENUMMER
        "06" -> FileStatusValidation.UGYLDIG_RECTYPE
        "09" -> FileStatusValidation.UGYLDIG_PRODDATO
        else -> FileStatusValidation.UKJENT
    }
}
