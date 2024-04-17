package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Session
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.util.Util.asMap

class TransaksjonRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource()
) {
    fun insert(transaksjon: Transaksjon, session: Session): Long? {
        return session.run(
            queryOf(
                """
                    INSERT INTO T_TRANSAKSJON  (
                        FIL_INFO_ID, 
                        K_TRANSAKSJON_S, 
                        PERSON_ID, 
                        K_BELOP_T, 
                        K_ART, 
                        K_ANVISER, 
                        FNR_FK, 
                        UTBETALES_TIL, 
                        DATO_FOM, 
                        DATO_TOM, 
                        DATO_ANVISER, 
                        DATO_PERSON_FOM, 
                        DATO_REAK_FOM, 
                        BELOP, 
                        REF_TRANS_ID, 
                        TEKSTKODE, 
                        RECTYPE, 
                        TRANS_EKS_ID_FK, 
                        K_TRANS_TOLKNING, 
                        SENDT_TIL_OPPDRAG,
                        FNR_ENDRET, 
                        MOT_ID, 
                        DATO_OPPRETTET, 
                        OPPRETTET_AV, 
                        DATO_ENDRET, 
                        ENDRET_AV, 
                        VERSJON, 
                        SALDO, 
                        KID, 
                        PRIORITET, 
                        K_TREKKANSVAR,
                        GRAD
                    ) VALUES (:filInfoId, :transaksjonStatus, :personId, :beloptype, :art, :anviser, :fnr, :utbetalesTil, :datoFom, :datoTom, :datoAnviser, :datoPersonFom, :datoReakFom, :belop, :refTransId, :tekstkode, :rectype, :transEksId, :transTolkning, :sendtTilOppdrag, :fnrEndret, :motId, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :saldo, :kid, :prioritet, :trekkansvar, :grad)
                """.trimIndent(),
                transaksjon.asMap()
            ).asUpdateAndReturnGeneratedKey
        )
    }

    fun updateTransaksjonTilstandId(transaksjonId: Int, transaksjonTilstandId: Int, session: Session) {
        session.run(
            queryOf(
                """
                    UPDATE T_TRANSAKSJON SET TRANS_TILSTAND_ID = $transaksjonTilstandId WHERE TRANSAKSJON_ID = $transaksjonId
                """.trimIndent()
            ).asUpdate
        )
    }
}

