package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.SECURE_LOGGER
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.isTransaksjonStatusOk
import no.nav.sokos.spk.mottak.domain.mapToTransaksjon
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.pdl.PdlService
import no.nav.sokos.spk.mottak.repository.AvvikTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.PersonRepository
import no.nav.sokos.spk.mottak.repository.READ_ROWS
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

private val logger = KotlinLogging.logger {}
private val secureLogger = KotlinLogging.logger(SECURE_LOGGER)
private const val UGYLDIG_FNR = "02"
private const val CHUNKED_SZIE = 1000

class ValidateTransaksjonService(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
    private val innTransaksjonRepository: InnTransaksjonRepository = InnTransaksjonRepository(dataSource),
    private val transaksjonRepository: TransaksjonRepository = TransaksjonRepository(dataSource),
    private val transaksjonTilstandRepository: TransaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource),
    private val avvikTransaksjonRepository: AvvikTransaksjonRepository = AvvikTransaksjonRepository(dataSource),
    private val personRepository: PersonRepository = PersonRepository(dataSource),
    private val pdlService: PdlService = PdlService(),
) {
    fun validateInnTransaksjon() {
        val timer = Instant.now()
        var totalInnTransaksjoner = 0
        var totalAvvikTransaksjoner = 0

        runCatching {
            logger.info { "Transaksjonsvalidering starter" }
            if (innTransaksjonRepository.getByBehandlet(rows = 1).isNotEmpty()) {
                validatePersonAndUpdateFnr()
                executeInntransaksjonValidation()

                while (true) {
                    val innTransaksjonList = innTransaksjonRepository.getByBehandlet()
                    totalInnTransaksjoner += innTransaksjonList.size
                    totalAvvikTransaksjoner += innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size
                    logger.debug { "Henter ${innTransaksjonList.size} inntransaksjoner fra databasen" }
                    when {
                        innTransaksjonList.isNotEmpty() -> saveTransaksjonAndAvvikTransaksjon(innTransaksjonList)
                        else -> break
                    }
                }

                when {
                    totalInnTransaksjoner > 0 -> {
                        val transTolkningIsNyMap = transaksjonRepository.getAllPersonIdWhereTranstolkningIsNyForMoreThanOneInstance()
                        if (transTolkningIsNyMap.isNotEmpty()) {
                            dataSource.transaction { session -> transaksjonRepository.updateAllWhereTranstolkningIsNyForMoreThanOneInstance(transTolkningIsNyMap.keys.toList(), session) }
                            logger.info { "Oppdatert personIder: ${transTolkningIsNyMap.keys.toList()} som har inntransaksjoner med samme personId og transTolkning = 'NY'" }
                        }

                        logger.info {
                            "$totalInnTransaksjoner inntransaksjoner validert på ${Duration.between(timer, Instant.now()).toSeconds()} sekunder. " +
                                "Opprettet ${totalInnTransaksjoner.minus(totalAvvikTransaksjoner)} transaksjoner og $totalAvvikTransaksjoner avvikstransaksjoner "
                        }
                    }

                    else -> logger.info { "Finner ingen inntransaksjoner som er ubehandlet" }
                }
                logger.info { "Transaksjonsvalidering avsluttet" }
            } else {
                logger.info { "Ingen inntransaksjoner å behandle" }
            }
        }.onFailure { exception ->
            val errorMessage = "Feil under behandling av inntransaksjoner. Feilmelding: ${exception.message}"
            logger.error(exception) { errorMessage }
            throw MottakException(errorMessage)
        }
    }

    private fun saveTransaksjonAndAvvikTransaksjon(innTransaksjonList: List<InnTransaksjon>) {
        val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOk() }

        dataSource.transaction { session ->
            innTransaksjonMap[true]?.takeIf { it.isNotEmpty() }?.apply {
                val personIdList = this.map { it.personId!! }
                val lastFagomraadeMap = innTransaksjonRepository.findLastFagomraadeByPersonId(personIdList)
                val lastTransaksjonMap =
                    runBlocking {
                        personIdList
                            .chunked(CHUNKED_SZIE)
                            .map { items ->
                                async(Dispatchers.IO) {
                                    transaksjonRepository.findLastTransaksjonByPersonId(items)
                                }
                            }.awaitAll()
                            .flatten()
                            .associateBy { it.personId }
                    }

                transaksjonRepository.insertBatch(this.map { it.mapToTransaksjon(lastTransaksjonMap[it.personId], lastFagomraadeMap) }, session)

                val transaksjonIdList = this.map { innTransaksjon -> innTransaksjon.innTransaksjonId!! }
                transaksjonTilstandRepository.insertBatch(transaksjonIdList = transaksjonIdList, session = session)
                transaksjonRepository.updateTransTilstandId(session)

                logger.debug { "${transaksjonIdList.size} transaksjoner opprettet" }
            }

            innTransaksjonMap[false]?.takeIf { it.isNotEmpty() }?.apply {
                val avvikTransaksjonIdList = avvikTransaksjonRepository.insertBatch(this, session)
                logger.debug { "${avvikTransaksjonIdList.size} avvikstransaksjoner opprettet" }
            }

            innTransaksjonRepository.updateBehandletStatusBatch(innTransaksjonList.map { it.innTransaksjonId!! }, session = session)
            innTransaksjonMap[true]?.let { Metrics.transaksjonGodkjentCounter.inc(it.size.toLong()) }
            innTransaksjonMap[false]?.let { Metrics.transaksjonAvvistCounter.inc(it.size.toLong()) }
        }
    }

    private fun validatePersonAndUpdateFnr() {
        dataSource.transaction { session ->
            innTransaksjonRepository.findAllFnrWithoutPersonId().chunked(READ_ROWS).forEach { fnrList ->
                runBlocking {
                    val identInformasjonMap = pdlService.getIdenterBolk(fnrList)
                    fnrList.forEach { fnr ->
                        val identInformasjon = identInformasjonMap[fnr]
                        if (!identInformasjon.isNullOrEmpty()) {
                            val ident = identInformasjon.first { !it.historisk }.ident
                            when {
                                identInformasjon.size == 1 -> personRepository.insert(ident, session)
                                else -> {
                                    val personList = personRepository.findByFnr(identInformasjon.filter { it.historisk }.map { it.ident })
                                    if (personList.size == 1) {
                                        personRepository.update(personList.first().personId!!, ident, session)
                                    } else {
                                        secureLogger.error { "Ingen person funnet i database for fnr: $fnr, ingen fnr oppdateres" }
                                        innTransaksjonRepository.updateTransaksjonStatus(fnr, UGYLDIG_FNR, session)
                                    }
                                }
                            }
                        } else {
                            secureLogger.error { "Ingen person funnet i PDL for fnr: $fnr" }
                            innTransaksjonRepository.updateTransaksjonStatus(fnr, UGYLDIG_FNR, session)
                        }
                    }
                }
            }
        }
    }

    private fun executeInntransaksjonValidation() {
        dataSource.transaction { session ->
            innTransaksjonRepository.validateTransaksjon(session)
        }
    }
}
