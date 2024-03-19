package no.nav.sokos.spk.mottak.database

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Statement
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import no.nav.sokos.spk.mottak.domain.FileInfo

object FileInfoRepository {

    fun Connection.updateFileState(
        fileState: String,
        fileType: String,
        id: Int
    ) =
        prepareStatement(
            """
                UPDATE T_FIL_INFO
                SET K_FIL_TILSTAND_T = (?)
                WHERE K_ANVISER = 'SPK'
                AND K_FIL_T = (?)
                AND FIL_INFO_ID = (?)
            """.trimIndent()
        ).withParameters(
            param(fileState),
            param(fileType),
            param(id)
        ).run {
            executeUpdate()
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
                FEILTEKST ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(), Statement.RETURN_GENERATED_KEYS
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
            return findId (generatedKeys)
        }

    private fun findId(rs: ResultSet): Int {
        while (rs.next()) {
            return rs.getLong(1).toInt()
        }
        throw SQLException("Finner ikke primary key i FIL_INFO")
    }
}