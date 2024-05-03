package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.util.Util.asMap

class FilInfoRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource()
) {
    fun getFilInfo(
        lopeNummer: Int,
        filTilstandType: String,
        anviser: String = "SPK"
    ): FilInfo? {
        return using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                        SELECT * FROM T_FIL_INFO WHERE LOPENR = :lopeNummer AND K_FIL_TILSTAND_T = :filTilstandType AND K_ANVISER = :anviser
                    """.trimIndent(),
                    mapOf(
                        "lopeNummer" to lopeNummer,
                        "filTilstandType" to filTilstandType,
                        "anviser" to anviser
                    )
                ), toFileInfo
            )
        }
    }

    fun updateFilInfoTilstandType(
        filInfoId: Int,
        filTilstandType: String,
        filType: String,
        session: Session
    ) {
        session.run(
            queryOf(
                """
                    UPDATE T_FIL_INFO
                    SET K_FIL_TILSTAND_T = (:filTilstandType), ENDRET_AV = '${PropertiesConfig.Configuration().naisAppName}', DATO_ENDRET = CURRENT_TIMESTAMP
                    WHERE K_ANVISER = 'SPK'
                    AND K_FIL_T = (:filType)
                    AND FIL_INFO_ID = (:filInfoId)
                """.trimIndent(),
                mapOf(
                    "filInfoId" to filInfoId,
                    "filTilstandType" to filTilstandType,
                    "filType" to filType
                )
            ).asUpdate
        )
    }

    fun insert(filInfo: FilInfo, session: Session): Long? {
        return session.run(
            queryOf(
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
                    FEILTEKST ) VALUES (:filStatus, :filTilstandType, :anviser, :filNavn, :lopeNr, :datoMottatt, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :filType, :feilTekst)
                """.trimIndent(),
                filInfo.asMap()
            ).asUpdateAndReturnGeneratedKey
        )
    }

    private val toFileInfo: (Row) -> FilInfo = { row ->
        FilInfo(
            row.int("FIL_INFO_ID"),
            row.string("K_FIL_S"),
            row.string("K_ANVISER"),
            row.string("K_FIL_T"),
            row.string("K_FIL_TILSTAND_T"),
            row.string("FIL_NAVN"),
            row.int("LOPENR"),
            row.stringOrNull("FEILTEKST"),
            row.localDateOrNull("DATO_MOTTATT"),
            row.localDateOrNull("DATO_SENDT"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON")
        )
    }
}