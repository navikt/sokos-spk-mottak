package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import no.nav.sokos.spk.mottak.modell.Transaksjon
import java.sql.Connection
import java.time.Instant

object InnTransaksjonRepository {

    fun Connection.insertTransaksjon(
        transaksjon: Transaksjon,
        filInfoId: Int
    ): Unit =
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
            param(transaksjon.transId),
            param(filInfoId),
            param("00"),
            param(transaksjon.gjelderId),
            param(transaksjon.belopsType),
            param(transaksjon.art),
            param("SPK"),
            param(transaksjon.utbetalesTil),
            param(transaksjon.periodeFOM),
            param(transaksjon.periodeTOM),
            param("02"),
            param(transaksjon.belop.toInt()),
            param(transaksjon.refTransId),
            param(transaksjon.tekstKode),
            param(transaksjon.datoAnviser),
            param(Instant.now().toString()),
            param("sokos.spk.mottak"),
            param(Instant.now().toString()),
            param("sokos.spk.mottak"),
            param(transaksjon.trekkansvar),
            param(transaksjon.kid),
            param(transaksjon.prioritet),
            param(transaksjon.saldo.toInt()),
            param(transaksjon.grad),
            param(2)
        ).run {
            executeUpdate()
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
}