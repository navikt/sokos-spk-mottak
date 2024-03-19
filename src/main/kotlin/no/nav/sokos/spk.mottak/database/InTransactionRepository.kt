package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import no.nav.sokos.spk.mottak.domain.record.TransactionRecord
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.Statement
import java.time.LocalDateTime

object InTransactionRepository {

    fun Connection.createInsertTransaction(): PreparedStatement =
        prepareStatement(
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
                GRAD_STR) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(), Statement.RETURN_GENERATED_KEYS
        )

    fun insertTransaction(
        ps: PreparedStatement,
        transactionRecord: TransactionRecord,
        fileInfoId: Int
    ) =
        ps.withParameters(
            param(fileInfoId),
            param("00"),
            param(transactionRecord.gjelderId),
            param(transactionRecord.belopsType),
            param(transactionRecord.art),
            param("SPK"),
            param(transactionRecord.utbetalesTil),
            param(transactionRecord.periodeFomStr),
            param(transactionRecord.periodeTomStr),
            param(transactionRecord.datoAnviserStr),
            param(transactionRecord.belopStr),
            param(""),
            param(""),
            param("02"),
            param(""),
            param(transactionRecord.periodeFom!!),
            param(transactionRecord.periodeTom!!),
            param(transactionRecord.datoAnviser!!),
            param(transactionRecord.belop!!),
            param("N"),
            param(LocalDateTime.now()),
            param("sokos.spk.mottak"),
            param(LocalDateTime.now()),
            param("sokos.spk.mottak"),
            param(1),  // TODO: Versjon?
            param(transactionRecord.prioritetStr),
            param(transactionRecord.trekkansvar),
            param(transactionRecord.saldoStr),
            param(transactionRecord.kid),
            param(transactionRecord.prioritet!!),
            param(transactionRecord.saldo!!),
            param(transactionRecord.grad!!),
            param(transactionRecord.gradStr),
        ).run {
            addBatch()
        }

    fun Connection.deleteTransactions(
        fileInfoId: Int
    ) = prepareStatement(
        """
            DELETE FROM T_INN_TRANSAKSJON 
            WHERE FIL_INFO_ID = (?)           
        """.trimIndent()
    ).withParameters(
        param(fileInfoId)
    ).run {
        executeUpdate()
    }
}
