package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.FILTILSTANDTYPE_GOD
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.util.Util.asMap

class FilInfoRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource(),
) {
    fun getByLopenummerAndFilTilstand(
        lopeNummer: Int,
        filTilstandType: String,
    ): FilInfo? {
        return using(sessionOf(dataSource)) { session ->
            session.single(
                queryOf(
                    """
                    SELECT * FROM T_FIL_INFO WHERE LOPENR = :lopeNummer AND K_FIL_TILSTAND_T = :filTilstandType AND K_ANVISER = 'SPK'
                    """.trimIndent(),
                    mapOf(
                        "lopeNummer" to lopeNummer,
                        "filTilstandType" to filTilstandType,
                    ),
                ),
                mapToFileInfo,
            )
        }
    }

    fun getByFilTilstandAndAllInnTransaksjonIsBehandlet(filTilstandType: String = FILTILSTANDTYPE_GOD): List<FilInfo> {
        return using(sessionOf(dataSource)) { session ->
            session.list(
                queryOf(
                    """
                    SELECT * FROM T_FIL_INFO
                    WHERE K_FIL_TILSTAND_T = '$filTilstandType'
                    AND K_FIL_S = '${FilStatus.OK.code}'
                    AND FIL_INFO_ID IN (select FIL_INFO_ID
                    FROM T_INN_TRANSAKSJON
                    GROUP BY FIL_INFO_ID
                    HAVING SUM(CASE WHEN BEHANDLET = 'J' THEN 0 ELSE 1 END) = 0)
                    """.trimIndent(),
                ),
                mapToFileInfo,
            )
        }
    }

    fun updateTilstandType(
        filInfoId: Int,
        filTilstandType: String,
        filType: String,
        session: Session,
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
                    "filType" to filType,
                ),
            ).asUpdate,
        )
    }

    fun insert(
        filInfo: FilInfo,
        session: Session,
    ): Long? {
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
                filInfo.asMap(),
            ).asUpdateAndReturnGeneratedKey,
        )
    }

    private val mapToFileInfo: (Row) -> FilInfo = { row ->
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
            row.int("VERSJON"),
        )
    }
}
