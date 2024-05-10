package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.repository.FilInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import java.io.File

private val logger = KotlinLogging.logger {}

class TransaksjonFileService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
    private val ftpService: FtpService = FtpService(),
) {
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource)
    private val filInfoRepository: FilInfoRepository = FilInfoRepository(dataSource)

    fun writeReturnFile() {
        runCatching {
            val filInfoIdList = filInfoRepository.getIdByFilTilstandAndAllTransaksjonIsBehandlet()
            if (filInfoIdList.isNotEmpty()) {
                logger.info { "Skriv fil til SPK for filInfoId: ${filInfoIdList.joinToString()}" }
                filInfoIdList.forEach {
                }
                logger.info { "Filene er opprettet og lastes opp til FTP server" }
            }
        }.onFailure { exception ->
            logger.error(exception) { "Feil under skriving fil til SPK" }
            throw MottakException("Feil under skriving fil til SPK")
        }
    }

    private fun createTempFileAndUploadToFtpServer() {
        dataSource.transaction { session ->
            File("")
        }
    }
}
