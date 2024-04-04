package no.nav.sokos.spk.mottak.service

import java.text.SimpleDateFormat
import java.util.Date
import kotliquery.TransactionalSession
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FilTilstandType
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.toFileInfo
import no.nav.sokos.spk.mottak.exception.ValidationException
import no.nav.sokos.spk.mottak.repository.FileInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.LopenummerRepository
import no.nav.sokos.spk.mottak.util.FileParser
import no.nav.sokos.spk.mottak.validator.FileStatus
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

        when {
            downloadFiles.isEmpty() -> logger.info { "Ingen filer å lese" }
            else -> logger.info { "Starter innlesning av antall filer: ${downloadFiles.size}" }
        }

        downloadFiles.forEach { (filename, content) ->
            lateinit var recordData: RecordData
            runCatching {
                DatabaseConfig.transaction { session ->
                    val maxLopenummer = lopenummerRepository.findMaxLopenummer(FILETYPE_ANVISER)

                    logger.info { "Leser inn fil: '$filename'" }
                    recordData = readRecords(content).apply {
                        this.maxLopenummer = maxLopenummer
                        this.filename = filename
                    }

                    validateStartAndEndRecord(recordData)
                    saveRecordDataAndMoveFile(recordData, session)
                }
            }.onFailure { exception ->
                when {
                    exception is ValidationException ->
                        DatabaseConfig.transaction { session ->
                            updateFileStatusAndUploadAvviksFil(recordData, exception, session)
                        }

                    else -> throw RuntimeException("Unknown exception", exception)
                }
            }
            logger.info { "Filen '$filename' er ferdig behandlet" }
        }
    }

    private fun saveRecordDataAndMoveFile(recordData: RecordData, session: TransactionalSession) {
        lopenummerRepository.updateLopenummer(recordData.startRecord.filLopenummer, FILETYPE_ANVISER, session)

        val filInfo = recordData.startRecord.toFileInfo(recordData.filename!!, FilTilstandType.GOD, FileStatus.OK.code)
        val filInfoId = fileInfoRepository.insertFilInfo(filInfo, session)!!

        var antallInnTransaksjon = 0
        recordData.innTransaksjonList.chunked(BATCH_SIZE).forEach { innTransaksjonList ->
            antallInnTransaksjon += innTransaksjonList.size
            innTransaksjonRepository.insertTransactionBatch(innTransaksjonList, filInfoId, session)
            logger.debug { "Antall innTransaksjon som er lagret i DB: $antallInnTransaksjon" }
        }
        recordData.innTransaksjonList.clear()
        logger.info { "Antall transaksjoner $antallInnTransaksjon lagt inn fra fil: ${recordData.filename} med løpenummer: ${recordData.startRecord.filLopenummer}" }
        ftpService.moveFile(recordData.filename!!, Directories.INBOUND, Directories.FERDIG)
    }

    private fun updateFileStatusAndUploadAvviksFil(
        recordData: RecordData,
        exception: ValidationException,
        session: TransactionalSession
    ) {
        lopenummerRepository.updateLopenummer(recordData.startRecord.filLopenummer, FILETYPE_ANVISER, session)

        val filInfo = recordData.startRecord.toFileInfo(recordData.filename!!, FilTilstandType.AVV, exception.statusCode, exception.message)
        fileInfoRepository.insertFilInfo(filInfo, session)!!

        createAvviksFil(recordData.startRecord.rawRecord, exception)
        logger.info { "Avviksfil er opprettet for fil: ${recordData.filename} med status: ${exception.statusCode} med løpenummer: ${recordData.startRecord.filLopenummer}" }
        ftpService.moveFile(recordData.filename!!, Directories.INBOUND, Directories.FERDIG)
    }

    private fun readRecords(content: List<String>): RecordData {
        var totalRecord = 0
        var totalBelop = 0L
        lateinit var startRecord: StartRecord
        lateinit var endRecord: EndRecord
        val innTransaksjons: MutableList<InnTransaksjon> = mutableListOf()

        content.forEach { record ->
            if (totalRecord++ == 0) {
                startRecord = FileParser.parseStartRecord(record)
                logger.debug { "Start-record: $record" }
            } else {
                if (content.size != totalRecord) {
                    val transaction = FileParser.parseTransaction(record)
                    totalBelop += transaction.belopStr.toLong()
                    innTransaksjons.add(transaction)
                } else {
                    logger.debug { "End-record: '$record'" }
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

    private fun createAvviksFil(startRecordUnparsed: String, exception: ValidationException) {
        val fileName = createFileName()
        val content = createAvviksRecord(startRecordUnparsed, exception)

        ftpService.createFile(fileName, Directories.ANVISNINGSRETUR, content)
    }

    private fun createAvviksRecord(startRecord: String, exception: ValidationException): String {
        return startRecord.replaceRange(76, 78, exception.statusCode)
            .replaceRange(78, 113, exception.message)

    }

    private fun createFileName(): String {
        return "SPK_NAV_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}_ANV"
    }
}
