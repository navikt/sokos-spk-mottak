package no.nav.sokos.spk.mottak.service

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.ANVISER_FIL_BESKRIVELSE
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_RET
import no.nav.sokos.spk.mottak.domain.FILTYPE_INNLEST
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.FilInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.util.FileParser
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

private val logger = KotlinLogging.logger {}
private const val OUTPUT_FILE_NAME = "SPK_NAV_%s_INL"

class WriteInnlesningsreturFileService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource),
    private val filInfoRepository: FilInfoRepository = FilInfoRepository(dataSource),
    private val ftpService: FtpService = FtpService(),
) {
    fun writeInnlesningsreturFile() {
        runCatching {
            val filInfoList = filInfoRepository.getByFilTilstandAndAllInnTransaksjonIsBehandlet()
            if (filInfoList.isNotEmpty()) {
                logger.info { "Returfil produseres for filInfoId: ${filInfoList.map { it.filInfoId }.joinToString()}" }
                filInfoList.forEach { createTempFileAndUploadToFtpServer(it) }
                logger.info { "Returfil for filInfoId: ${filInfoList.map { it.filInfoId }.joinToString()} er produsert og lastet opp til FTP server" }
            }
        }.onFailure { exception ->
            val errorMessage = "Skriving av returfil feilet. Feilmelding: ${exception.message}"
            logger.error(exception) { errorMessage }
            throw MottakException(errorMessage)
        }
    }

    private fun createTempFileAndUploadToFtpServer(filInfo: FilInfo) {
        dataSource.transaction { session ->
            val innTransaksjonList = innTransaksjonRepository.getByFilInfoId(filInfo.filInfoId!!)

            val returFilnavn = generateFileName()
            val anvisningFil =
                StringBuilder(
                    FileParser.createStartRecord(
                        anviser = filInfo.anviser,
                        lopeNr = filInfo.lopeNr,
                        filType = FILTYPE_INNLEST,
                        datoMottatt = filInfo.datoMottatt!!,
                        beskrivelse = ANVISER_FIL_BESKRIVELSE,
                    ),
                )
            var antallTransaksjon = 0
            var sumBelop = 0L

            innTransaksjonList.forEach { innTransaksjon ->
                anvisningFil.append(FileParser.createTransaksjonRecord(innTransaksjon))
                antallTransaksjon++
                sumBelop += innTransaksjon.belop.toLong()
            }
            anvisningFil.append(FileParser.createSluttRecord(antallTransaksjon + 2, sumBelop))

            val applicationNavn = PropertiesConfig.Configuration().naisAppName
            filInfoRepository.insert(
                FilInfo(
                    filInfoId = null,
                    filStatus = FilStatus.OK.code,
                    anviser = SPK,
                    filType = FILTYPE_INNLEST,
                    filTilstandType = FILTILSTANDTYPE_RET,
                    filNavn = returFilnavn,
                    lopeNr = filInfo.lopeNr,
                    feilTekst = null,
                    datoMottatt = filInfo.datoMottatt,
                    datoSendt = null,
                    datoOpprettet = LocalDateTime.now(),
                    opprettetAv = applicationNavn,
                    datoEndret = LocalDateTime.now(),
                    endretAv = applicationNavn,
                    versjon = 1,
                ),
                session,
            )
            innTransaksjonRepository.deleteByFilInfoId(filInfo.filInfoId, session)

            ftpService.createFile(returFilnavn, Directories.ANVISNINGSRETUR, anvisningFil.toString())
            logger.info { "$returFilnavn med $antallTransaksjon transaksjoner og beløp: $sumBelop er opprettet og lastet opp til ${Directories.ANVISNINGSRETUR}" }
            Metrics.innlesningsreturCounter.inc(antallTransaksjon.toLong())
        }
    }

    private fun generateFileName(): String = OUTPUT_FILE_NAME.format(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS")))
}
