package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import java.sql.Connection
import java.sql.ResultSet

object LopenummerRepository {

    fun Connection.findMaxLopenummer(
        fileType: String
    ): Int =
        prepareStatement(
            """
                SELECT SISTE_LOPENR 
                FROM  T_LOPENR
                WHERE K_ANVISER = 'SPK'
                AND K_FIL_T = (?)
            """.trimIndent()
        ).withParameters(
            param(fileType)
        ).run {
            executeQuery().findMax()
        }

    fun Connection.updateLopenummer(
        lopenummer: Int,
        filtype: String
    ) =
        prepareStatement(
            """
                UPDATE T_LOPENR 
                SET SISTE_LOPENR = (?)
                WHERE K_ANVISER = 'SPK'
                AND K_FIL_T = (?)
            """.trimIndent()
        ).withParameters(
            param(lopenummer),
            param(filtype)
        ).run {
            executeUpdate()
        }


    private fun ResultSet.findMax() = run {
        if (next()) getInt(1)
        else 0
    }
}