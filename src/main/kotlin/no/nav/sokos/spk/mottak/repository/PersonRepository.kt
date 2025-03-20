package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.Person
import no.nav.sokos.spk.mottak.domain.VALIDATE_TRANSAKSJON_SERVICE
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics

class PersonRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) {
    private val findByFnrTimer = Metrics.timer(DATABASE_CALL, "PersonRepository", "findByFnr")
    private val insertTimer = Metrics.timer(DATABASE_CALL, "PersonRepository", "insert")
    private val updateTimer = Metrics.timer(DATABASE_CALL, "PersonRepository", "update")

    fun findByFnr(fnrList: List<String>): List<Person> =
        using(sessionOf(dataSource)) { session ->
            findByFnrTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT PERSON_ID, FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON
                        FROM T_PERSON
                        WHERE FNR_FK IN (${fnrList.joinToString(separator = "','", prefix = "'", postfix = "'")})
                        """.trimIndent(),
                    ),
                    mapToPerson,
                )
            }
        }

    fun insert(
        fnr: String,
        session: Session,
    ): Long? {
        val systemId = VALIDATE_TRANSAKSJON_SERVICE
        return insertTimer.recordCallable {
            session.updateAndReturnGeneratedKey(
                queryOf(
                    """
                    INSERT INTO T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON)
                    VALUES (:fnr, CURRENT_TIMESTAMP, :opprettetAv, CURRENT_TIMESTAMP, :endretAv, 1)
                    """.trimIndent(),
                    mapOf(
                        "fnr" to fnr,
                        "opprettetAv" to systemId,
                        "endretAv" to systemId,
                    ),
                ),
            )
        }
    }

    fun update(
        personId: Int,
        fnr: String,
        session: Session,
    ) {
        updateTimer.recordCallable {
            val systemId = VALIDATE_TRANSAKSJON_SERVICE
            session.update(
                queryOf(
                    """
                    UPDATE T_PERSON
                    SET FNR_FK = :fnr, DATO_ENDRET = CURRENT_TIMESTAMP, ENDRET_AV = :endretAv, VERSJON = VERSJON + 1
                    WHERE PERSON_ID = :personId
                    """.trimIndent(),
                    mapOf(
                        "personId" to personId,
                        "fnr" to fnr,
                        "endretAv" to systemId,
                    ),
                ),
            )
        }
    }

    private val mapToPerson: (Row) -> Person = { row ->
        Person(
            row.int("PERSON_ID"),
            row.string("FNR_FK"),
            row.localDateTime("DATO_OPPRETTET"),
            row.string("OPPRETTET_AV"),
            row.localDateTime("DATO_ENDRET"),
            row.string("ENDRET_AV"),
            row.int("VERSJON"),
        )
    }
}
