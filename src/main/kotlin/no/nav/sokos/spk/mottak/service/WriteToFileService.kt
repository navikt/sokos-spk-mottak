package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_RET
import no.nav.sokos.spk.mottak.domain.FILTYPE_INNLEST
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.repository.FilInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.util.FileParser
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val logger = KotlinLogging.logger {}
private const val OUTPUT_FILE_NAME = "SPK_NAV_%s_ANV"

class WriteToFileService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource),
    private val filInfoRepository: FilInfoRepository = FilInfoRepository(dataSource),
    private val ftpService: FtpService = FtpService(),
) {
    fun writeReturnFile() {
        runCatching {
            val filInfoList = filInfoRepository.getByFilTilstandAndAllInnTransaksjonIsBehandlet()
            if (filInfoList.isNotEmpty()) {
                logger.info { "Returfil produseres for filInfoId: ${filInfoList.map { it.filInfoId }.joinToString()}" }
                filInfoList.forEach { createTempFileAndUploadToFtpServer(it) }
                logger.info { "Returfil er produsert og lastet opp til FTP server" }
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
            var anvisningFil = FileParser.createStartRecord(filInfo)
            var antallTransaksjon = 0
            var sumBelop = 0

            innTransaksjonList.map { innTransaksjon ->
                anvisningFil += FileParser.createTransaksjonRecord(innTransaksjon)
                antallTransaksjon++
                sumBelop += innTransaksjon.belop
            }
            anvisningFil += FileParser.createEndRecord(antallTransaksjon+2, sumBelop)

            filInfoRepository.insert(
                filInfo.copy(
                    filInfoId = null,
                    filType = FILTYPE_INNLEST,
                    filTilstandType = FILTILSTANDTYPE_RET,
                    filNavn = returFilnavn,
                    feilTekst = null,
                    datoOpprettet = LocalDateTime.now(),
                    datoEndret = LocalDateTime.now(),
                ),
                session,
            )
            innTransaksjonRepository.deleteByFilInfoId(filInfo.filInfoId, session)

            ftpService.createFile(returFilnavn, Directories.ANVISNINGSRETUR, anvisningFil)
            logger.info { "$returFilnavn med antall transaksjoner: $antallTransaksjon og beløp: $sumBelop opprettet og har lastet opp til ${Directories.ANVISNINGSRETUR}" }
        }
    }

    private fun generateFileName(): String = OUTPUT_FILE_NAME.format(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")))
}
