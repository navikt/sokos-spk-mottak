package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.util.Util.asMap

class AvvikTransaksjonRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource()
) {
    fun insertBatch(innTransaksjonList: List<InnTransaksjon>, session: Session): List<Int> {
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
                    PRIORITET, 
                    SALDO, 
                    TREKKANSVAR,                    
                    KID,         
                    GRAD
                ) VALUES (:innTransaksjonId, :filInfoId, :transaksjonStatus, :fnr, :belopstype, :art, :avsender, :utbetalesTil, :datoFomStr, :datoTomStr, :datoAnviserStr, :belopStr, :refTransId, :tekstkode, :rectype, :transId, CURRENT_TIMESTAMP, '$systemId', CURRENT_TIMESTAMP, '$systemId', :versjon, :prioritetStr, :saldoStr, :trekkansvar, :kid, :grad) 
            """.trimIndent(),
            innTransaksjonList.map { it.asMap() }
        )
    }
}