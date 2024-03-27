package no.nav.sokos.spk.mottak.repository

import java.time.LocalDateTime
import javax.sql.DataSource
import kotliquery.Session
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.record.InnTransaksjon
import no.nav.sokos.spk.mottak.util.StringUtil.toLocalDate

class InnTransaksjonRepository(
    private val dataSource: DataSource = DatabaseConfig.hikariDataSource()
) {
    fun insertTransactionBatch(innTransaksjonList: List<InnTransaksjon>, filInfoId: Long, session: Session) {
        session.batchPreparedNamedStatement(
            """
                INSERT INTO T_INN_TRANSAKSJON (
                FIL_INFO_ID, 
                K_TRANSAKSJON_S,
                FNR_FK,
                BELOPSTYPE,
                ART,
                AVSENDER,
                UTBETALES_TIL,
                DATO_FOM_STR,
                DATO_TOM_STR,
                DATO_ANVISER_STR,
                BELOP_STR,
                REF_TRANS_ID,
                TEKSTKODE,
                RECTYPE,
                TRANS_ID_FK,
                DATO_FOM,
                DATO_TOM,
                DATO_ANVISER,
                BELOP,
                BEHANDLET,
                DATO_OPPRETTET,
                OPPRETTET_AV,
                DATO_ENDRET,
                ENDRET_AV,
                VERSJON,
                PRIORITET_STR,
                TREKKANSVAR,
                SALDO_STR,
                KID,
                PRIORITET,
                SALDO,
                GRAD,
                GRAD_STR) VALUES (:filInfoId, :transaksjonStatus, :fnr, :belopstype, :art, :avsender, :utbetalesTil, :datoFomStr, :datoTomStr, :datoAnviserStr, :belopStr, :refTransId, :tekstkode, :rectype, :transId, :datoFom, :datoTom, :datoAnviser, :belop, :behandlet, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :prioritetStr, :trekkansvar, :saldoStr, :kid, :prioritet, :saldo, :grad, :gradStr)
            """.trimIndent(),
            innTransaksjonList.convertToListMap(filInfoId)
        )
    }


    private fun List<InnTransaksjon>.convertToListMap(filInfoId: Long): List<Map<String, Any?>> {
        return this.map { innTransaksjon ->
            mapOf(
                "filInfoId" to filInfoId,
                "transaksjonStatus" to "00",
                "fnr" to innTransaksjon.fnr,
                "belopstype" to innTransaksjon.belopstype,
                "art" to innTransaksjon.art,
                "avsender" to SPK,
                "utbetalesTil" to innTransaksjon.utbetalesTil,
                "datoFomStr" to innTransaksjon.datoFomStr,
                "datoTomStr" to innTransaksjon.datoTomStr,
                "datoAnviserStr" to innTransaksjon.datoAnviserStr,
                "belopStr" to innTransaksjon.belopStr,
                "refTransId" to "",
                "tekstkode" to "",
                "rectype" to "02",
                "transId" to "",
                "datoFom" to innTransaksjon.datoFomStr.toLocalDate(),
                "datoTom" to innTransaksjon.datoTomStr.toLocalDate(),
                "datoAnviser" to innTransaksjon.datoAnviserStr.toLocalDate(),
                "belop" to innTransaksjon.belopStr.toIntOrNull(),
                "behandlet" to "N",
                "datoOpprettet" to LocalDateTime.now(),
                "opprettetAv" to PropertiesConfig.Configuration().naisAppName,
                "datoEndret" to LocalDateTime.now(),
                "endretAv" to PropertiesConfig.Configuration().naisAppName,
                "versjon" to "1",  // TODO: Versjon? Trenger vi dette
                "prioritetStr" to innTransaksjon.prioritetStr,
                "trekkansvar" to innTransaksjon.trekkansvar,
                "saldoStr" to innTransaksjon.saldoStr,
                "kid" to innTransaksjon.kid,
                "prioritet" to innTransaksjon.prioritetStr.toLocalDate(),
                "saldo" to innTransaksjon.saldoStr.toIntOrNull(),
                "grad" to innTransaksjon.gradStr.toIntOrNull(),
                "gradStr" to innTransaksjon.gradStr
            )
        }
    }
}

