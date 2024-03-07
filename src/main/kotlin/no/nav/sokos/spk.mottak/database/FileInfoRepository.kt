package no.nav.sokos.spk.mottak.database

import com.ibm.db2.jcc.am.SqlException
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import java.math.BigDecimal
import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException

object FileInfoRepository {

    fun Connection.findMaxLopenummer(
        anviser: String
    ): Int =
        prepareStatement(
            """
                SELECT MAX(FIL_INFO_ID) 
                FROM  T_FIL_INFO
                WHERE K_ANVISER = (?)
            """.trimIndent()
        ).withParameters(
            param(anviser)
        ).run {
            executeQuery().findMax()
        }

    fun Connection.updateFileState(
        fileState: String
    ) =
        prepareStatement(
            """
                UPDATE T_FIL_INFO
                SET K_FIL_TILSTAND_T = (?)
            """.trimIndent()
        ).withParameters(
            param(fileState)
        ).run {
            executeQuery()
            close()
            commit()
        }

    fun Connection.insertFile(
        file: FileInfo
    ): Int =
        prepareStatement(
            """
                INSERT INTO T_FIL_INFO (
                K_FIL_S,
                K_FIL_TILSTAND_T,
                K_ANVISER,
                FIL_NAVN,
                LOPENR,
                DATO_MOTTATT,
                DATO_OPPRETTET,
                OPPRETTET_AV,
                DATO_ENDRET,
                ENDRET_AV,
                VERSJON,
                K_FIL_T,
                FEILTEKST ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?), Statement.RETURN_GENERATED_KEYS
            """.trimIndent()
        ).withParameters(
            param(file.status),
            param(file.tilstand),
            param(file.anviser),
            param(file.filnavn),
            param(file.lopenr),
            param(file.datoMottatt),
            param(file.datoOpprettet),
            param(file.opprettetAv),
            param(file.datoEndret),
            param(file.endretAv),
            param(file.versjon),
            param(file.filType),
            param(file.feilTekst)
        ).run {
            executeUpdate()
            val id = findId (generatedKeys)
            close()
            commit()
            id
        }

    private fun findId(rs: ResultSet): Int {
        while (rs.next()) {
            return rs.getBigDecimal(1).intValueExact()
        }
        throw SQLException("Can't get primary key")
    }

    private fun ResultSet.findMax() = run {
        if (next()) getInt(1)
        else 0
    }
}