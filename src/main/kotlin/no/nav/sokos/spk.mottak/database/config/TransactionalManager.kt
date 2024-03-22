package no.nav.sokos.spk.mottak.database.config

import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using

object TransactionalManager {
    fun <A> transaction(operation: (TransactionalSession) -> A): A {
        val dataSource = HikariConfig.hikariDataSource()
        return using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.transaction {
                operation(it)
            }
        }
    }
}