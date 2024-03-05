package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import no.nav.sokos.spk.mottak.modell.Transaction
import java.sql.Connection
import java.sql.PreparedStatement
import java.time.Instant

object InTransactionRepository {

    fun Connection.insertTransaction(
        transaction: Transaction,
        fileInfoId: Int
    ): PreparedStatement =
        prepareStatement(
            """
                INSERT INTO T_INN_TRANSAKSJON (
                INN_TRANSAKSJON_ID,
                FIL_INFO_ID,
                K_TRANSAKSJON_S,
                FNR_FK,
                BELOPSTYPE,
                ART,
                AVSENDER,
                UTBETALES_TIL,
                DATO_FOM,
                DATO_TOM,
                RECTYPE,
                BELOP,
                REF_TRANS_ID,
                TEKSTKODE,
                DATO_ANVISER,
                DATO_OPPRETTET,
                OPPRETTET_AV,
                DATO_ENDRET,
                ENDRET_AV,
                TREKKANSVAR,
                KID,
                PRIORITET,
                SALDO,
                GRAD,
                VERSJON) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).withParameters(
            param(transaction.transId),
            param(fileInfoId),
            param("00"),
            param(transaction.gjelderId),
            param(transaction.belopsType),
            param(transaction.art),
            param("SPK"),
            param(transaction.utbetalesTil),
            param(transaction.periodeFOM),
            param(transaction.periodeTOM),
            param("02"),
            param(transaction.belop.toInt()),
            param(transaction.refTransId),
            param(transaction.tekstKode),
            param(transaction.datoAnviser),
            param(Instant.now().toString()),
            param("sokos.spk.mottak"),
            param(Instant.now().toString()),
            param("sokos.spk.mottak"),
            param(transaction.trekkansvar),
            param(transaction.kid),
            param(transaction.prioritet),
            param(transaction.saldo.toInt()),
            param(transaction.grad),
            param(2)
        ).run {
            addBatch()
            this
        }
}

// Ukjente felter:
//    DATO_FOM_STR,
//    DATO_TOM_STR,
//    DATO_ANVISER_STR,
//    BELOP_STR,
//    TRANS_ID_FK,
//    BEHANDLET, HVIS en transaksjon godkjennes skal den få status '00' og den skal lagres i tabellen TRANSAKSJON med aktuell statuskode OG behandlet = true
//    PRIORITET_STR,
//    SALDO_STR,
//    GRAD_STR
