package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.AvvikTransaksjon
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.util.SQLUtils.asMap

class AvvikTransaksjonRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    fun insertBatch(
        innTransaksjonList: List<InnTransaksjon>,
        session: Session,
    ): List<Int> {
        val systemId = PropertiesConfig.Configuration().naisAppName
        return session.batchPreparedNamedStatement(
            """
            INSERT INTO T_AVV_TRANSAKSJON (
                AVV_TRANSAKSJON_ID,
                FIL_INFO_ID, 
                K_TRANSAKSJON_S, 
                FNR_FK, 
                BELOPSTYPE, 
                ART, 
                AVSENDER, 
                UTBETALES_TIL, 
                DATO_FOM, 
                DATO_TOM, 
                DATO_ANVISER, 
                BELOP, 
                REF_TRANS_ID, 
                TEKSTKODE, 
                RECTYPE, 
                TRANS_EKS_ID_FK, 
                DATO_OPPRETTET, 
                OPPRETTET_AV,
                DATO_ENDRET, 
                ENDRET_AV, 
                VERSJON,   
                GRAD
            ) VALUES (:innTransaksjonId, :filInfoId, :transaksjonStatus, :fnr, :belopstype, :art, :avsender, :utbetalesTil, :datoFomStr, :datoTomStr, :datoAnviserStr, :belopStr, :refTransId, :tekstkode, :rectype, :transId, CURRENT_TIMESTAMP, '$systemId', CURRENT_TIMESTAMP, '$systemId', :versjon, :grad) 
            """.trimIndent(),
            innTransaksjonList.map { it.asMap() },
        )
    }

    /**
     * Bruker kun for testing
     */
    fun getByAvvTransaksjonId(avvikTransaksjonId: Int): AvvikTransaksjon? =
        using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                    SELECT AVV_TRANSAKSJON_ID, FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_EKS_ID_FK, DATO_OPPRETTET, OPPRETTET_AV,
                               DATO_ENDRET, ENDRET_AV, VERSJON, PRIORITET, SALDO, TREKKANSVAR, KID, GRAD
                    FROM T_AVV_TRANSAKSJON 
                    WHERE AVV_TRANSAKSJON_ID = :avvikTransaksjonId
                    """.trimIndent(),
                    mapOf("avvikTransaksjonId" to avvikTransaksjonId),
                ),
                mapToAvvikTransaksjon,
            )
        }

    private val mapToAvvikTransaksjon: (Row) -> AvvikTransaksjon = { row ->
        AvvikTransaksjon(
            avvikTransaksjonId = row.int("AVV_TRANSAKSJON_ID"),
            filInfoId = row.int("FIL_INFO_ID"),
            transaksjonStatus = row.string("K_TRANSAKSJON_S"),
            fnr = row.string("FNR_FK"),
            belopType = row.string("BELOPSTYPE"),
            art = row.string("ART"),
            avsender = row.string("AVSENDER"),
            utbetalesTil = row.stringOrNull("UTBETALES_TIL"),
            datoFom = row.string("DATO_FOM"),
            datoTom = row.string("DATO_TOM"),
            datoAnviser = row.string("DATO_ANVISER"),
            belop = row.string("BELOP"),
            refTransId = row.stringOrNull("REF_TRANS_ID"),
            tekstKode = row.stringOrNull("TEKSTKODE"),
            rectType = row.string("RECTYPE"),
            transEksId = row.string("TRANS_EKS_ID_FK"),
            datoOpprettet = row.localDateTime("DATO_OPPRETTET"),
            opprettetAv = row.string("OPPRETTET_AV"),
            datoEndret = row.localDateTime("DATO_ENDRET"),
            endretAv = row.string("ENDRET_AV"),
            versjon = row.int("VERSJON"),
            grad = row.intOrNull("GRAD"),
        )
    }
}
