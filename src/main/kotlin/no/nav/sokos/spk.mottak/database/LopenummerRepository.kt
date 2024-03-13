package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import java.sql.Connection

object LopenummerRepository {

    fun Connection.updateLopenummer(
        lopenummer: Int,
        anviser: String,
        filtype: String
    ) =
        prepareStatement(
            """
                UPDATE T_LOPENR 
                SET SISTE_LOPENR = (?)
                WHERE K_ANVISER = (?)
                AND K_FIL_T = (?)
            """.trimIndent()
        ).withParameters(
            param(lopenummer),
            param(anviser),
            param(filtype)
        ).run {
            executeUpdate()
        }
}