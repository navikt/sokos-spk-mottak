package no.nav.sokos.spk.mottak.service

import java.sql.SQLException
import java.sql.SQLTimeoutException
import java.time.Clock
import java.time.Duration
import java.util.Collections
import java.util.IdentityHashMap

import mu.KotlinLogging

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.TilstandsRyddeKonfigurasjon
import no.nav.sokos.spk.mottak.domain.RyddeStopp
import no.nav.sokos.spk.mottak.domain.RyddeStoppgrunn
import no.nav.sokos.spk.mottak.domain.SeksMaanedersRegel
import no.nav.sokos.spk.mottak.domain.TilstandsRyddeOppsummering
import no.nav.sokos.spk.mottak.domain.TilstandsRyddeRegel
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.metrics.SERVICE_CALL
import no.nav.sokos.spk.mottak.repository.Kjoringslas
import no.nav.sokos.spk.mottak.repository.RyddeBatchresultat
import no.nav.sokos.spk.mottak.repository.RyddeEnhetskontroll
import no.nav.sokos.spk.mottak.repository.RyddeUavklartUtfall
import no.nav.sokos.spk.mottak.repository.TilstandsRyddeDatakilde
import no.nav.sokos.spk.mottak.repository.TilstandsRyddeStatus
import no.nav.sokos.spk.mottak.repository.TilstandsRyddeStatuslager

private val logger = KotlinLogging.logger {}

class TilstandsRyddeService(
    private val db2: TilstandsRyddeDatakilde,
    private val statuslager: TilstandsRyddeStatuslager,
    private val konfigurasjon: TilstandsRyddeKonfigurasjon = PropertiesConfig.tilstandsRyddeKonfigurasjon(),
    private val regel: TilstandsRyddeRegel = SeksMaanedersRegel(konfigurasjon.oppbevaringMaaneder),
    private val klokke: Clock = Clock.systemUTC(),
    private val nanoTime: () -> Long = System::nanoTime,
    private val vent: (Duration) -> Unit = { Thread.sleep(it) },
    private val stoppindikator: () -> Boolean = { false },
    private val avslutningsobservator: (TilstandsRyddeOppsummering) -> Unit = { resultat ->
        logger.info {
            "Tilstandsrydding avsluttet utfall=${resultat.stoppgrunn.utfall} kandidater=${resultat.kandidater} " +
                "slettede=${resultat.slettede} nullstilte=${resultat.nullstilteReferanser} varighet=${resultat.varighet}"
        }
    },
) {
    init {
        require(regel.oppbevaringMaaneder == konfigurasjon.oppbevaringMaaneder)
    }

    fun kjor(): TilstandsRyddeOppsummering = Metrics.timer(SERVICE_CALL, "TilstandsRyddeService", "kjor").recordCallable { gjennomfor() }!!

    private fun gjennomfor(): TilstandsRyddeOppsummering {
        val struping = RyddeStruping(konfigurasjon, nanoTime, vent, stoppindikator)
        var kandidater = 0L
        var slettede = 0L
        var nullstilte = 0L
        var aktiv = false
        var utfall = RyddeStoppgrunn.FEIL
        var opprinneligFeil: Exception? = null

        fun oppsummering(grunn: RyddeStoppgrunn): TilstandsRyddeOppsummering = TilstandsRyddeOppsummering(kandidater, slettede, nullstilte, struping.varighet(), grunn)

        fun opptell(
            fase: String,
            resultat: RyddeBatchresultat,
        ) {
            slettede += resultat.slettede.toLong()
            nullstilte += resultat.nullstilteReferanser.toLong()
            Metrics.tilstandsryddingSlettede.labelValues(fase).inc(resultat.slettede.toLong())
            Metrics.tilstandsryddingNullstilteReferanser.inc(resultat.nullstilteReferanser.toLong())
        }

        try {
            struping.kontroller()
            val kontroll = RyddeEnhetskontroll(struping::timeoutSekunder)
            val opprinneligLas = postgresEnhet { statuslager.provKjoringslas(kontroll) }
            if (opprinneligLas == null) {
                struping.kontroller()
                utfall = RyddeStoppgrunn.OPPTATT
                Metrics.tilstandsryddingKjoringer.labelValues("opptatt").inc()
                return oppsummering(RyddeStoppgrunn.OPPTATT)
            }
            val las =
                object : Kjoringslas {
                    override fun kontroller(timeoutSekunder: Int) {
                        postgresEnhet { opprinneligLas.kontroller(timeoutSekunder) }
                    }

                    override fun close() {
                        postgresEnhet { opprinneligLas.close() }
                    }
                }
            val resultat =
                medKjoringslas(las) {
                    struping.kontroller()
                    aktiv = true
                    Metrics.tilstandsryddingAktiv.set(1.0)
                    las.kontroller(kontroll.timeoutSekunder())
                    kontroll.timeoutSekunder()
                    var status = postgresEnhet { statuslager.hent(opprinneligLas, kontroll) }
                    struping.kontroller()
                    status?.let(::oppdaterStatusmaalinger)
                    if (status == null ||
                        status.fullfort ||
                        status.regelversjon != regel.versjon ||
                        status.oppbevaringMaaneder != regel.oppbevaringMaaneder
                    ) {
                        val snapshot = db2Enhet(las, struping, kontroll) { db2.hentSnapshot(kontroll) }
                        val tom = snapshot.minId == null
                        status =
                            TilstandsRyddeStatus(
                                regel.versjon,
                                regel.oppbevaringMaaneder,
                                regel.grensetidspunkt(snapshot.start),
                                snapshot.minId?.toLong(),
                                snapshot.maxId?.toLong(),
                                null,
                                tom,
                                if (tom) klokke.instant() else status?.sisteFullfortTidspunkt,
                            )
                        lagreStatus(las, opprinneligLas, status, kontroll)
                        if (tom) {
                            return@medKjoringslas oppsummering(RyddeStoppgrunn.FULLFORT)
                        }
                    }
                    var rundestatus = checkNotNull(status)
                    var fraId = rundestatus.sisteFerdigeId?.plus(1) ?: rundestatus.forsteId!!
                    val ovreId = rundestatus.ovreId!!
                    while (fraId <= ovreId) {
                        val tilId = minOf(fraId + konfigurasjon.intervallBredde.toLong() - 1, ovreId)
                        val grense = rundestatus.grensetidspunkt
                        logger.info { "Rydder intervall fra=$fraId til=$tilId regel=${regel.versjon} grense=$grense" }
                        for (fase in listOf("urefererte", "resterende")) {
                            val ider =
                                db2Enhet(las, struping, kontroll, { funnet: List<Int> ->
                                    kandidater += funnet.size.toLong()
                                    Metrics.tilstandsryddingKandidater.labelValues(fase).inc(funnet.size.toLong())
                                }) {
                                    db2.finnKandidater(fraId, tilId, regel, grense, kontroll)
                                }
                            ider.chunked(konfigurasjon.batchStorrelse).forEach { batch ->
                                db2Enhet<RyddeBatchresultat>(las, struping, kontroll, { opptell(fase, it) }) {
                                    if (fase == "urefererte") {
                                        db2.slettUrefererte(batch, regel, grense, kontroll)
                                    } else {
                                        db2.nullstillOgSlett(batch, regel, grense, konfigurasjon.maksReferanserPerBatch, kontroll)
                                    }
                                }
                            }
                        }
                        val fullfort = tilId == ovreId
                        rundestatus =
                            rundestatus.copy(
                                sisteFerdigeId = tilId,
                                fullfort = fullfort,
                                sisteFullfortTidspunkt = if (fullfort) klokke.instant() else rundestatus.sisteFullfortTidspunkt,
                            )
                        lagreStatus(las, opprinneligLas, rundestatus, kontroll)
                        fraId = tilId + 1
                    }
                    if (!rundestatus.fullfort) {
                        rundestatus = rundestatus.copy(fullfort = true, sisteFullfortTidspunkt = klokke.instant())
                        lagreStatus(las, opprinneligLas, rundestatus, kontroll)
                    }
                    oppsummering(RyddeStoppgrunn.FULLFORT)
                }
            utfall = resultat.stoppgrunn
            Metrics.tilstandsryddingKjoringer.labelValues(resultat.stoppgrunn.utfall).inc()
            return resultat
        } catch (stopp: RyddeStopp) {
            utfall = stopp.grunn
            Metrics.tilstandsryddingKjoringer.labelValues(stopp.grunn.utfall).inc()
            return oppsummering(stopp.grunn)
        } catch (feil: Exception) {
            opprinneligFeil = feil
            Metrics.tilstandsryddingKjoringer.labelValues("feil").inc()
            // Ikke logg JDBC-melding, årsakskjede eller SQL med mulige personopplysninger.
            val sqlfeil = feilkjede(feil).filterIsInstance<SQLException>().firstOrNull()
            logger.error { "Tilstandsrydding feilet SQLSTATE=${sqlfeil?.sqlState} SQLCODE=${sqlfeil?.errorCode}" }
            throw feil
        } finally {
            if (aktiv) Metrics.tilstandsryddingAktiv.set(0.0)
            try {
                avslutningsobservator(oppsummering(utfall))
            } catch (observatorFeil: Exception) {
                if (opprinneligFeil != null) {
                    opprinneligFeil.addSuppressed(observatorFeil)
                } else {
                    throw observatorFeil
                }
            }
        }
    }

    private fun lagreStatus(
        las: Kjoringslas,
        opprinneligLas: Kjoringslas,
        status: TilstandsRyddeStatus,
        kontroll: RyddeEnhetskontroll,
    ) {
        las.kontroller(kontroll.timeoutSekunder())
        kontroll.timeoutSekunder()
        postgresEnhet { statuslager.lagre(opprinneligLas, status, kontroll) }
        if (status.fullfort) Metrics.tilstandsryddingRunderFullfort.inc()
        kontroll.timeoutSekunder()
        oppdaterStatusmaalinger(status)
    }

    private fun oppdaterStatusmaalinger(status: TilstandsRyddeStatus) {
        Metrics.tilstandsryddingMarkor.set((status.sisteFerdigeId ?: 0).toDouble())
        Metrics.tilstandsryddingOvre.set((status.ovreId ?: 0).toDouble())
        Metrics.tilstandsryddingSisteFullfort.set(status.sisteFullfortTidspunkt?.let { it.epochSecond + it.nano / 1e9 } ?: 0.0)
    }

    private fun <T> postgresEnhet(handling: () -> T): T {
        try {
            return handling()
        } catch (feil: Exception) {
            val kjede = feilkjede(feil)
            val sqlfeil = kjede.filterIsInstance<SQLException>()
            if (sqlfeil.isNotEmpty()) {
                val type =
                    when {
                        kjede.any { it is SQLTimeoutException } || sqlfeil.any { it.sqlState in setOf("57014", "HYT00", "HYT01") } -> "timeout"
                        sqlfeil.any { it.sqlState in setOf("23503", "23504", "23506") } -> "fremmednokkel"
                        sqlfeil.any { it.sqlState in setOf("40001", "57033", "55P03", "40P01") } -> "las"
                        else -> "annen"
                    }

                Metrics.tilstandsryddingSqlfeil.labelValues(type).inc()
            }
            throw feil
        }
    }

    private fun <T> medKjoringslas(
        las: Kjoringslas,
        handling: () -> T,
    ): T {
        var opprinneligFeil: Throwable? = null
        try {
            return handling()
        } catch (feil: Throwable) {
            opprinneligFeil = feil
            throw feil
        } finally {
            try {
                las.close()
            } catch (frigjoringsfeil: Throwable) {
                val original = opprinneligFeil
                if (original == null) throw frigjoringsfeil
                if (original is RyddeStopp) {
                    // En kontrollert stopp må ikke skjule en mislykket frigjøring.
                    if (frigjoringsfeil !== original) frigjoringsfeil.addSuppressed(original)
                    throw frigjoringsfeil
                }
                if (frigjoringsfeil !== original) original.addSuppressed(frigjoringsfeil)
            }
        }
    }

    private fun <T> db2Enhet(
        las: Kjoringslas,
        struping: RyddeStruping,
        kontroll: RyddeEnhetskontroll,
        etterCommit: (T) -> Unit = {},
        handling: () -> T,
    ): T {
        for (forsok in 1..konfigurasjon.maksForsok) {
            las.kontroller(kontroll.timeoutSekunder())
            kontroll.timeoutSekunder()
            val start = nanoTime()
            val resultat: T
            try {
                resultat = handling()
            } catch (feil: Exception) {
                if (feil is RyddeStopp) throw feil
                val kjede = feilkjede(feil)
                val sqlfeil = kjede.filterIsInstance<SQLException>()
                val godkjenteLasetilstander = setOf("40001", "57033")
                val lasefeil =
                    sqlfeil.any { it.sqlState in godkjenteLasetilstander } &&
                        sqlfeil.all { it.sqlState == null || it.sqlState in godkjenteLasetilstander } &&
                        kjede.size < 64
                val timeout = kjede.any { it is SQLTimeoutException } || sqlfeil.any { it.sqlState in setOf("57014", "HYT00", "HYT01") }
                val fremmednokkel = sqlfeil.any { it.sqlState in setOf("23503", "23504", "23506") }
                if (sqlfeil.isNotEmpty()) {
                    val type =
                        when {
                            timeout -> "timeout"
                            fremmednokkel -> "fremmednokkel"
                            lasefeil -> "las"
                            else -> "annen"
                        }
                    Metrics.tilstandsryddingSqlfeil.labelValues(type).inc()
                }
                val provIgjen =
                    lasefeil &&
                        !timeout &&
                        !fremmednokkel &&
                        kjede.none { it is RyddeUavklartUtfall } &&
                        forsok < konfigurasjon.maksForsok
                try {
                    pause(struping, start, lasefeil)
                } catch (pausefeil: Exception) {
                    if (provIgjen && pausefeil is RyddeStopp) {
                        pausefeil.addSuppressed(feil)
                        throw pausefeil
                    }
                    feil.addSuppressed(pausefeil)
                    throw feil
                }
                if (!provIgjen) throw feil
                continue
            }
            // Tell bare bekreftede commits, og tell før en pause kan stoppe kjøringen.
            etterCommit(resultat)
            pause(struping, start, false)
            return resultat
        }
        error("Ingen forsøk tilgjengelig")
    }

    private fun pause(
        struping: RyddeStruping,
        start: Long,
        lasefeil: Boolean,
    ) {
        val varighet = Duration.ofNanos((nanoTime() - start).coerceAtLeast(0))
        try {
            struping.etterEnhet(varighet, lasefeil)
        } finally {
            Metrics.tilstandsryddingPause.set(struping.pause.toNanos() / 1e9)
        }
    }

    private fun feilkjede(feil: Throwable): List<Throwable> {
        val sett = Collections.newSetFromMap(IdentityHashMap<Throwable, Boolean>())
        val ko = ArrayDeque<Throwable>()
        val resultat = mutableListOf<Throwable>()
        ko.add(feil)
        while (ko.isNotEmpty() && resultat.size < 64) {
            val neste = ko.removeFirst()
            if (!sett.add(neste)) continue
            resultat += neste
            neste.cause?.let(ko::add)
            if (neste is SQLException) neste.nextException?.let(ko::add)
        }
        return resultat
    }
}
