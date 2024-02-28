package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.database.RepositoryExtensions.param
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.withParameters
import java.sql.Connection

object FilInfoRepository {

    fun Connection.updateTilstand(
        tilstand: String
    ): Unit =
        prepareStatement(
            """
                UPDATE K_FIL_INFO
                SET K_FIL_TILSTAND_T = (?)
            """.trimIndent()
        ).withParameters(
            param(tilstand)
        ).run {
            executeUpdate()
        }

    fun Connection.insertFil(
        fil: FilInfo
    ): Unit =
        prepareStatement(
            """
                INSERT INTO K_FIL_INFO (
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
                FEILTEKST ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        ).withParameters(
            param(fil.id),
            param(fil.status),
            param(fil.tilstand),
            param(fil.anviser),
            param(fil.filnavn),
            param(fil.lopenr),
            param(fil.datoMottatt),
            param(fil.datoOpprettet),
            param(fil.opprettetAv),
            param(fil.datoEndret),
            param(fil.endretAv),
            param(fil.versjon),
            param(fil.filType),
            param(fil.feilTekst)
        ).run {
            executeUpdate()
        }
}