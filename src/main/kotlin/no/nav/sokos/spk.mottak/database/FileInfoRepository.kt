package no.nav.sokos.spk.mottak.database

import kotliquery.Session
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.database.config.HikariConfig
import no.nav.sokos.spk.mottak.domain.FilInfo
import javax.sql.DataSource

class FileInfoRepository(
    private val dataSource: DataSource = HikariConfig.hikariDataSource()
) {
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
                    SET K_FIL_TILSTAND_T = (:filTilstandType)
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
                        FEILTEKST ) VALUES (:status, :tilstand, :anviser, :filnavn, :lopenr, :datoMottatt, :datoOpprettet, :opprettetAv, :datoEndret, :endretAv, :versjon, :filType, :feilTekst)
                    """.trimIndent(),
                mapOf(
                    "status" to filInfo.status,
                    "tilstand" to filInfo.tilstand,
                    "anviser" to filInfo.anviser,
                    "filnavn" to filInfo.filnavn,
                    "lopenr" to filInfo.lopenr,
                    "datoMottatt" to filInfo.datoMottatt,
                    "datoOpprettet" to filInfo.datoOpprettet,
                    "opprettetAv" to filInfo.opprettetAv,
                    "datoEndret" to filInfo.datoEndret,
                    "endretAv" to filInfo.endretAv,
                    "versjon" to filInfo.versjon,
                    "filType" to filInfo.filType,
                    "feilTekst" to filInfo.feilTekst
                )
            ).asUpdateAndReturnGeneratedKey
        )
    }
}