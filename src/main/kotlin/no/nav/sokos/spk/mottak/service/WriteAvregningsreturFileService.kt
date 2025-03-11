package no.nav.sokos.spk.mottak.service

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

import com.zaxxer.hikari.HikariDataSource
import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.AVREGNING_FIL_BESKRIVELSE
import no.nav.sokos.spk.mottak.domain.Avregningsretur
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_RET
import no.nav.sokos.spk.mottak.domain.FILTYPE_AVREGNING
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.AvregningsreturRepository
import no.nav.sokos.spk.mottak.repository.FilInfoRepository
import no.nav.sokos.spk.mottak.repository.LopenummerRepository
import no.nav.sokos.spk.mottak.util.FileParser
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

private val logger = KotlinLogging.logger {}
private val BATCH_SIZE = 20000
private const val OUTPUT_FILE_NAME = "SPK_NAV_%s_AVR"

class WriteAvregningsreturFileService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
    private val avregningsreturRepository: AvregningsreturRepository = AvregningsreturRepository(dataSource),
    private val filInfoRepository: FilInfoRepository = FilInfoRepository(dataSource),
    private val lopenummerRepository: LopenummerRepository = LopenummerRepository(dataSource),
    private val ftpService: FtpService = FtpService(),
) {
    fun writeAvregningsreturFile() {
        runCatching {
            val avregningTransaksjonList = avregningsreturRepository.getReturTilAnviserWhichIsNotSent()
            if (avregningTransaksjonList.isNotEmpty()) {
                logger.info { "Returfil produseres for ${avregningTransaksjonList.size} avregningstransaksjoner" }
                val filInfoInnId = createTempFileAndUploadToFtpServer(avregningTransaksjonList)
                logger.info { "Returfil for avregning til filInfoInnId: $filInfoInnId er produsert og lastet opp til FTP server" }
            } else {
                logger.info { "Ingen avregningstransaksjoner blir funnet!" }
            }
        }.onFailure { exception ->
            val errorMessage = "Skriving av avregningfil feilet. Feilmelding: ${exception.message}"
            logger.error(exception) { errorMessage }
            throw MottakException(errorMessage)
        }
    }

    private fun createTempFileAndUploadToFtpServer(avregningTransaksjonList: List<Avregningsretur>): Long {
        return dataSource.transaction { session ->
            val applicationNavn = PropertiesConfig.Configuration().naisAppName

            val lopeNummer = lopenummerRepository.findMaxLopeNummer(FILTYPE_AVREGNING)?.inc() ?: 1
            lopenummerRepository.updateLopeNummer(lopeNummer.toString(), FILTYPE_AVREGNING, session)

            val returFilnavn = OUTPUT_FILE_NAME.format(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmssSSS")))
            val filInfoId =
                filInfoRepository.insert(
                    FilInfo(
                        filInfoId = null,
                        filStatus = FilStatus.OK.code,
                        anviser = SPK,
                        filType = FILTYPE_AVREGNING,
                        filTilstandType = FILTILSTANDTYPE_RET,
                        filNavn = returFilnavn,
                        lopeNr = lopeNummer.toString().padStart(6, '0'),
                        feilTekst = null,
                        datoMottatt = LocalDate.now(),
                        datoSendt = null,
                        datoOpprettet = LocalDateTime.now(),
                        opprettetAv = applicationNavn,
                        datoEndret = LocalDateTime.now(),
                        endretAv = applicationNavn,
                        versjon = 1,
                    ),
                    session,
                )!!

            val avregningFil =
                StringBuilder(
                    FileParser.createStartRecord(
                        anviser = SPK,
                        lopeNr = lopeNummer.toString().padStart(6, '0'),
                        filType = FILTYPE_AVREGNING,
                        datoMottatt = LocalDate.now(),
                        beskrivelse = AVREGNING_FIL_BESKRIVELSE,
                    ),
                )

            var antallTransaksjon = 0
            var sumBelop = 0L

            avregningTransaksjonList.forEach { transaksjon ->
                avregningFil.append(FileParser.createAvregningTransaksjonRecord(transaksjon))
                antallTransaksjon++
                sumBelop += transaksjon.belop.toLong()
            }
            avregningFil.append(FileParser.createSluttRecord(antallTransaksjon + 2, sumBelop))

            avregningTransaksjonList.chunked(BATCH_SIZE) { transaksjoner ->
                avregningsreturRepository.updateBatch(transaksjoner.map { it.returTilAnviserId!! }, filInfoId.toInt(), session)
            }

            ftpService.createFile(returFilnavn, Directories.AVREGNINGSRETUR, avregningFil.toString())
            logger.info { "$returFilnavn med $antallTransaksjon transaksjoner og beløp: ${sumBelop / 100} er opprettet og lastet opp til ${Directories.ANVISNINGSRETUR}" }
            Metrics.avregningsreturCounter.inc(antallTransaksjon.toLong())

            filInfoId
        }
    }
}
