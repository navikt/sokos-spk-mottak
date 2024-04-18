package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.FilInfo

class FileInfoRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.dataSource()
) {
    fun getFileInfo(lopenummer: Int, filTilstandType: String): FilInfo? {
        return sessionOf(dataSource).single(
            queryOf(
                """
                    SELECT * FROM T_FIL_INFO WHERE LOPENR = :lopenummer AND K_FIL_TILSTAND_T = :filTilstandType
                """.trimIndent(),
                mapOf(
                    "lopenummer" to lopenummer,
                    "filTilstandType" to filTilstandType)
            ), toFileInfo
        )
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

    fun insertFilInfo(filInfo: FilInfo, session: Session): Long? {
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
                        FEILTEKST ) VALUES (:filStatus, :filTilstandType, :anviser, :filNavn, :lopenr, :datoMottatt, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :filType, :feiltekst)
                    """.trimIndent(),
                mapOf(
                    "filStatus" to filInfo.filStatus,
                    "filTilstandType" to filInfo.filTilstandType,
                    "anviser" to filInfo.anviser,
                    "filNavn" to filInfo.filNavn,
                    "lopenr" to filInfo.lopenr,
                    "datoMottatt" to filInfo.datoMottatt,
                    "datoOpprettet" to filInfo.datoOpprettet,
                    "opprettetAv" to filInfo.opprettetAv,
                    "datoEndret" to filInfo.datoEndret,
                    "endretAv" to filInfo.endretAv,
                    "versjon" to filInfo.versjon,
                    "filType" to filInfo.filType,
                    "feiltekst" to filInfo.feiltekst
                )
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
            row.localDate("DATO_MOTTATT"),
            row.localDateOrNull("DATO_SENDT"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON")
        )
    }
}