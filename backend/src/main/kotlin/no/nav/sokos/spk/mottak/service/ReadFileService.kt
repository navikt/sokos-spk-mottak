package no.nav.sokos.spk.mottak.service

import java.text.SimpleDateFormat
import java.util.Date

import com.zaxxer.hikari.HikariDataSource
import kotliquery.TransactionalSession
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_AVV
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.FILTYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.READ_FILE_SERVICE
import no.nav.sokos.spk.mottak.domain.record.RecordData
import no.nav.sokos.spk.mottak.domain.record.SluttRecord
import no.nav.sokos.spk.mottak.domain.record.StartRecord
import no.nav.sokos.spk.mottak.domain.record.TransaksjonRecord
import no.nav.sokos.spk.mottak.domain.record.toFileInfo
import no.nav.sokos.spk.mottak.exception.FilValidationException
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.FilInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.LopenummerRepository
import no.nav.sokos.spk.mottak.util.FileParser
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction
import no.nav.sokos.spk.mottak.validator.FileValidation.validateStartAndSluttRecord

private const val BATCH_SIZE: Int = 20000
private val logger = KotlinLogging.logger {}

class ReadFileService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource),
    private val lopenummerRepository: LopenummerRepository = LopenummerRepository(dataSource),
    private val filInfoRepository: FilInfoRepository = FilInfoRepository(dataSource),
    private val ftpService: FtpService = FtpService(),
) {
    fun readAndParseFile() {
        if (innTransaksjonRepository.countByInnTransaksjon() > 0) {
            logger.info { "Forrige kjøring av inntransaksjoner er ikke ferdig og derfor vil ikke innlesningsprosessen starte" }
            return
        }

        val downloadFiles = ftpService.downloadFiles()

        when {
            downloadFiles.isEmpty() -> logger.info { "Ingen filer er lastet ned" }
            else -> logger.info { "Lastet ned ${downloadFiles.size} filer" }
        }

        downloadFiles.forEach { (filename, content) ->
            lateinit var recordData: RecordData
            runCatching {
                dataSource.transaction { session ->
                    val maxLopenummer = lopenummerRepository.findMaxLopeNummer(FILTYPE_ANVISER)

                    logger.info { "Leser inn fil: $filename" }
                    recordData =
                        readRecords(content).apply {
                            this.maxLopenummer = maxLopenummer
                            this.filNavn = filename
                        }

                    if (recordData.filStatus == FilStatus.OK) {
                        validateStartAndSluttRecord(recordData)
                        saveRecordDataAndMoveFile(recordData, session)
                    } else {
                        throw FilValidationException(recordData.filStatus)
                    }
                }
            }.onFailure { exception ->
                when {
                    exception is FilValidationException ->
                        dataSource.transaction { session ->
                            updateFileStatusAndUploadAvviksFil(recordData, exception, session)
                        }

                    else -> {
                        val errorMessage = "Ukjent feil ved innlesing av fil: $filename. Feilmelding: ${exception.message}"
                        throw MottakException(errorMessage)
                    }
                }
            }
        }
    }

    private fun saveRecordDataAndMoveFile(
        recordData: RecordData,
        session: TransactionalSession,
    ) {
        lopenummerRepository.updateLopeNummer(recordData.startRecord.filLopenummer, FILTYPE_ANVISER, READ_FILE_SERVICE, session)

        val filInfo = recordData.startRecord.toFileInfo(recordData.filNavn!!, FILTILSTANDTYPE_GOD, FilStatus.OK.code, null, READ_FILE_SERVICE)
        val filInfoId = filInfoRepository.insert(filInfo, session)!!

        var antallInnTransaksjon = 0
        recordData.transaksjonRecordList.chunked(BATCH_SIZE).forEach { innTransaksjonList ->
            antallInnTransaksjon += innTransaksjonList.size
            innTransaksjonRepository.insertBatch(innTransaksjonList, filInfoId, session)
            logger.debug { "$antallInnTransaksjon inntransaksjoner lagret i databasen" }
        }
        recordData.transaksjonRecordList.clear()
        ftpService.moveFile(recordData.filNavn!!, Directories.INBOUND, Directories.ANVISNINGSFIL_BEHANDLET)

        logger.info { "${recordData.filNavn} med løpenummer: ${recordData.startRecord.filLopenummer} er ferdigbehandlet. $antallInnTransaksjon inntransaksjoner har blitt lagt inn fra fil" }
        Metrics.fileProcessedCounter.inc()
        Metrics.innTransaksjonCounter.inc(antallInnTransaksjon.toLong())
    }

    private fun updateFileStatusAndUploadAvviksFil(
        recordData: RecordData,
        exception: FilValidationException,
        session: TransactionalSession,
    ) {
        runCatching {
            if (exception.statusCode != FilStatus.FILLOPENUMMER_I_BRUK.code &&
                exception.statusCode != FilStatus.FORVENTET_FILLOPENUMMER.code &&
                exception.statusCode != FilStatus.UGYLDIG_ANVISER.code &&
                exception.statusCode != FilStatus.UGYLDIG_FILTYPE.code
            ) {
                lopenummerRepository.updateLopeNummer(recordData.startRecord.filLopenummer, FILTYPE_ANVISER, READ_FILE_SERVICE, session)
            } else {
                logger.error { "Kan ikke oppdatere løpenummer for ${recordData.filNavn} siden betingelsene ikke er tilfredsstilt" }
            }

            val filInfo =
                recordData.startRecord.toFileInfo(
                    recordData.filNavn!!,
                    FILTILSTANDTYPE_AVV,
                    exception.statusCode,
                    exception.message,
                    READ_FILE_SERVICE,
                )
            filInfoRepository.insert(filInfo, session)!!

            createAvviksFil(recordData.startRecord.kildeData, exception)
            ftpService.moveFile(recordData.filNavn!!, Directories.INBOUND, Directories.ANVISNINGSFIL_BEHANDLET)
            logger.info { "Avviksfil er opprettet for fil: ${recordData.filNavn} med status: ${exception.statusCode} og løpenummer: ${recordData.startRecord.filLopenummer}" }
        }.onFailure {
            val errorMessage = "Feil ved opprettelse av avviksfil: ${recordData.filNavn}. Feilmelding: ${it.message}"
            logger.error { errorMessage }
            throw MottakException(errorMessage)
        }
    }

    private fun readRecords(content: List<String>): RecordData {
        var totalRecord = 0
        var totalBelop = 0L
        lateinit var startRecord: StartRecord
        lateinit var sluttRecord: SluttRecord
        val transaksjonRecordList: MutableList<TransaksjonRecord> = mutableListOf()
        var filStatus = FilStatus.OK

        content.forEach { record ->
            if (totalRecord++ == 0) {
                startRecord = FileParser.parseStartRecord(record)
                if (filStatus == FilStatus.OK && startRecord.filStatus != FilStatus.OK) {
                    filStatus = startRecord.filStatus
                }
                logger.debug { "Start-record: $record" }
            } else {
                if (content.size != totalRecord) {
                    val transaction = FileParser.parseTransaksjonRecord(record)
                    if (filStatus == FilStatus.OK && transaction.filStatus != FilStatus.OK) {
                        filStatus = transaction.filStatus
                    }
                    totalBelop += transaction.belop.toLongOrNull() ?: 0L
                    transaksjonRecordList.add(transaction)
                } else {
                    logger.debug { "End-record: $record" }
                    sluttRecord = FileParser.parseSluttRecord(record)
                    if (filStatus == FilStatus.OK && sluttRecord.filStatus != FilStatus.OK) {
                        filStatus = sluttRecord.filStatus
                    }
                }
            }
        }
        return RecordData(
            startRecord = startRecord,
            sluttRecord = sluttRecord,
            transaksjonRecordList = transaksjonRecordList,
            totalBelop = totalBelop,
            filStatus = filStatus,
        )
    }

    private fun createAvviksFil(
        startRecordUnparsed: String,
        exception: FilValidationException,
    ) {
        val fileName = createFileName()
        val content = createAvviksRecord(startRecordUnparsed, exception)

        ftpService.createFile(fileName = fileName, Directories.ANVISNINGSRETUR, content)
    }

    private fun createAvviksRecord(
        startRecord: String,
        exception: FilValidationException,
    ): String =
        startRecord
            .padEnd(113, ' ')
            .replaceRange(76, 78, exception.filStatus.code)
            .replaceRange(78, 113, exception.filStatus.decode.padEnd(35, ' '))

    private fun createFileName(): String = "SPK_NAV_${SimpleDateFormat("yyyyMMdd_HHmmssSSS").format(Date())}_INL"
}
