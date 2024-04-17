package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import java.text.SimpleDateFormat
import java.util.Date
import kotliquery.TransactionalSession
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_AVV
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.record.EndRecord
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
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
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource(),
    private val ftpService: FtpService = FtpService()
) {
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource)
    private val lopenummerRepository: LopenummerRepository = LopenummerRepository(dataSource)
    private val fileInfoRepository: FileInfoRepository = FileInfoRepository(dataSource)

    fun readAndParseFile() {
        val downloadFiles = ftpService.downloadFiles()

        when {
            downloadFiles.isEmpty() -> logger.info { "Ingen filer å lese" }
            else -> logger.info { "Starter innlesning av antall filer: ${downloadFiles.size}" }
        }

        downloadFiles.forEach { (filename, content) ->
            lateinit var recordData: RecordData
            runCatching {
                dataSource.transaction { session ->
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
                        dataSource.transaction() { session ->
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

        val filInfo = recordData.startRecord.toFileInfo(recordData.filename!!, FILTILSTANDTYPE_GOD, FileStatus.OK.code)
        val filInfoId = fileInfoRepository.insertFilInfo(filInfo, session)!!

        var antallInnTransaksjon = 0
        recordData.transaksjonRecordList.chunked(BATCH_SIZE).forEach { innTransaksjonList ->
            antallInnTransaksjon += innTransaksjonList.size
            innTransaksjonRepository.insertBatch(innTransaksjonList, filInfoId, session)
            logger.debug { "Antall innTransaksjon som er lagret i DB: $antallInnTransaksjon" }
        }
        recordData.transaksjonRecordList.clear()
        logger.info { "Antall transaksjoner $antallInnTransaksjon lagt inn fra fil: ${recordData.filename} med løpenummer: ${recordData.startRecord.filLopenummer}" }
        ftpService.moveFile(recordData.filename!!, Directories.INBOUND, Directories.FERDIG)
    }

    private fun updateFileStatusAndUploadAvviksFil(
        recordData: RecordData,
        exception: ValidationException,
        session: TransactionalSession
    ) {
        lopenummerRepository.updateLopenummer(recordData.startRecord.filLopenummer, FILETYPE_ANVISER, session)

        val filInfo = recordData.startRecord.toFileInfo(
            recordData.filename!!,
            FILTILSTANDTYPE_AVV,
            exception.statusCode,
            exception.message
        )
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
        val transaksjonRecordList: MutableList<TransaksjonRecord> = mutableListOf()

        content.forEach { record ->
            if (totalRecord++ == 0) {
                startRecord = FileParser.parseStartRecord(record)
                logger.debug { "Start-record: $record" }
            } else {
                if (content.size != totalRecord) {
                    val transaction = FileParser.parseTransaction(record)
                    totalBelop += transaction.belop.toLong()
                    transaksjonRecordList.add(transaction)
                } else {
                    logger.debug { "End-record: '$record'" }
                    endRecord = FileParser.parseEndRecord(record)
                }
            }
        }
        return RecordData(
            startRecord = startRecord,
            endRecord = endRecord,
            transaksjonRecordList = transaksjonRecordList,
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
            .replaceRange(78, startRecord.length, exception.message)

    }

    private fun createFileName(): String {
        return "SPK_NAV_${SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())}_ANV"
    }
}
