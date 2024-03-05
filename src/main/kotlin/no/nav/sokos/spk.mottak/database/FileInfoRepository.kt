package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import java.sql.Connection

object FileInfoRepository {

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
            executeUpdate()
            commit()
        }

    fun Connection.insertFile(
        file: FileInfo
    ) =
        prepareStatement(
            """
                INSERT INTO T_FIL_INFO (
                FIL_INFO_ID,
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
                FEILTEKST ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).withParameters(
            param(file.id),
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
            commit()
        }
}