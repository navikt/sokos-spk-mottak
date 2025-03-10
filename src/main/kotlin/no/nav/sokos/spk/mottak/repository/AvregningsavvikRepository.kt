package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.AvregningsgrunnlagAvvik
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.util.SQLUtils.asMap

class AvregningsavvikRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) {
    private val insertTimer = Metrics.timer(DATABASE_CALL, "AvregningsavvikRepository", "insert")

    fun insert(
        avregningsgrunnlagAvvik: AvregningsgrunnlagAvvik,
        feilmelding: String,
        session: Session,
    ): Long? {
        val systemId = PropertiesConfig.Configuration().naisAppName
        return insertTimer.recordCallable {
            session.updateAndReturnGeneratedKey(
                queryOf(
                    """
                    INSERT INTO T_AVREGNING_AVVIK (
                    OPPDRAGS_ID,
                    LINJE_ID,
                    TREKKVEDTAK_ID,
                    GJELDER_ID,
                    UTBETALES_TIL,
                    DATO_STATUS_SATT,
                    STATUS,
                    BILAGSNR_SERIE,
                    BILAGSNR,
                    KONTO,
                    FOM_DATO,
                    TOM_DATO,
                    BELOP,
                    DEBET_KREDIT,
                    UTBETALING_TYPE,
                    TRANS_TEKST,
                    DATO_VALUTERT,
                    DELYTELSE_ID,
                    FAGSYSTEM_ID,
                    KREDITOR_REF,
                    FEILMELDING,
                    DATO_OPPRETTET,
                    OPPRETTET_AV,
                    DATO_ENDRET,
                    ENDRET_AV ) VALUES (:oppdragsId, :linjeId, :trekkvedtakId, :gjelderId, :utbetalesTil, :datoStatusSatt, :status, :bilagsnrSerie, :bilagsnr, :konto, :fomdato, :tomdato, :belop, :debetKredit, :utbetalingsType, :transTekst, :datoValutert, :delytelseId, :fagSystemId, :kreditorRef, '$feilmelding', CURRENT_TIMESTAMP, '$systemId', CURRENT_TIMESTAMP, '$systemId')
                    """.trimIndent(),
                    avregningsgrunnlagAvvik.asMap(),
                ),
            )
        }
    }

    // Kun for testing
    fun getNoOfRows(): Int? =
        using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf("SELECT COUNT(*) FROM T_AVREGNING_AVVIK"),
            ) { row -> row.int(1) }
        }

    fun getFeilmeldingByBilagsNr(
        bilagsNrSerie: String,
        bilagsNr: String,
    ): String? =
        using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                    SELECT FEILMELDING
                    FROM T_AVREGNING_AVVIK 
                    WHERE BILAGSNR_SERIE = '$bilagsNrSerie' AND BILAGSNR = '$bilagsNr'
                    """.trimIndent(),
                ),
            ) { row -> row.string("FEILMELDING") }
        }
}
