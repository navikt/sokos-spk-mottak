package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import no.nav.sokos.spk.mottak.modell.Transaction
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.LocalDateTime

object InTransactionRepository {

    fun Connection.insertTransaction(
        transaction: Transaction,
        fileInfoId: Int
    ): PreparedStatement =
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
                RECTYPE,
                DATO_FOM,
                DATO_TOM,
                DATO_ANVISER,
                BELOP,
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
                GRAD_STR) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?), Statement.RETURN_GENERATED_KEYS
            """.trimIndent()
        ).withParameters(
            param(fileInfoId),
            param("00"),
            param(transaction.gjelderId),
            param(transaction.belopsType),
            param(transaction.art),
            param("SPK"),
            param(transaction.utbetalesTil),
            param(transaction.periodeFomStr),
            param(transaction.periodeTomStr),
            param(transaction.datoAnviserStr),
            param(transaction.belopStr),
            param("02"),
            param(transaction.periodeFom!!),
            param(transaction.periodeTom!!),
            param(transaction.datoAnviser!!),
            param(transaction.belop!!),
            param(LocalDateTime.now()),
            param("sokos.spk.mottak"),
            param(LocalDateTime.now()),
            param("sokos.spk.mottak"),
            param(1),  // TODO: Versjon?
            param(transaction.prioritetStr),
            param(transaction.trekkansvar),
            param(transaction.saldoStr),
            param(transaction.kid),
            param(transaction.prioritet!!),
            param(transaction.saldo!!),
            param(transaction.grad!!),
            param(transaction.gradStr),
        ).run {
            addBatch()
            this
        }
}
