package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.avregning.Avregningsretur
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.util.SQLUtils.asMap

class AvregningsreturRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) {
    private val insertTimer = Metrics.timer(DATABASE_CALL, "AvregningsreturRepository", "insert")

    fun insert(
        avregningsretur: Avregningsretur,
        session: Session,
    ): Long? =
        insertTimer.recordCallable {
            session.updateAndReturnGeneratedKey(
                queryOf(
                    """
                    INSERT INTO T_RETUR_TIL_ANV (
                    RECTYPE,
                    K_RETUR_T,
                    K_ANVISER,
                    OS_ID_FK,
                    OS_LINJE_ID_FK,
                    TREKKVEDTAK_ID_FK,
                    GJELDER_ID,
                    FNR_FK,
                    DATO_STATUS,
                    STATUS,
                    BILAGSNR_SERIE,
                    BILAGSNR,
                    DATO_FOM,
                    DATO_TOM,
                    BELOP,
                    DEBET_KREDIT,
                    UTBETALING_TYPE,
                    TRANS_TEKST,
                    TRANS_EKS_ID_FK,
                    DATO_AVSENDER,
                    UTBETALES_TIL,
                    STATUS_TEKST,
                    RETURTYPE_KODE,
                    DUPLIKAT,
                    TRANSAKSJON_ID,
                    FIL_INFO_INN_ID,
                    FIL_INFO_UT_ID,
                    DATO_VALUTERING,
                    KONTO,
                    MOT_ID,
                    DATO_OPPRETTET,
                    OPPRETTET_AV,
                    DATO_ENDRET,
                    ENDRET_AV,
                    VERSJON ) VALUES (:rectype, :returtype, :anviser, :osId, :osLinjeId, :trekkvedtakId, :gjelderId, :fnr, :datoStatus, :status, :bilagsNrSerie, :bilagsNr, :datoFom, :datoTom, :belop, :debetKredit, :utbetalingType, :transaksjonTekst, :transEksId, :datoAvsender, :utbetalesTil, :statusTekst, :returtypeKode, :duplikat, :datoValutering, :konto, :motId, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon,)
                    """.trimIndent(),
                    avregningsretur.asMap(),
                ),
            )
        }
}
