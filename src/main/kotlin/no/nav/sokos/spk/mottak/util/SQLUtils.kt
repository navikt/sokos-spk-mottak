package no.nav.sokos.spk.mottak.util

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using
import kotlin.reflect.full.memberProperties

object SQLUtils {
    inline fun <reified T : Any> Row.optionalOrNull(columnLabel: String): T? {
        return runCatching {
            this.any(columnLabel) as? T
        }.getOrNull()
    }

    inline fun <reified T : Any> T.asMap(): Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }

    fun <A> HikariDataSource.transaction(operation: (TransactionalSession) -> A): A =
        using(sessionOf(this, returnGeneratedKey = true)) { session ->
            session.transaction { tx ->
                operation(tx)
            }
        }
}
