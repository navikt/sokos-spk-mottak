package no.nav.sokos.spk.mottak.util

import java.sql.SQLException
import java.util.concurrent.ThreadLocalRandom

import kotlin.reflect.full.memberProperties

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

object SQLUtils {
    inline fun <reified T : Any> Row.optionalOrNull(columnLabel: String): T? =
        runCatching {
            this.any(columnLabel) as? T
        }.getOrNull()

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

    fun <A> HikariDataSource.transactionWithDeadlockRetry(operation: (TransactionalSession) -> A): A {
        var attempt = 1
        while (true) {
            try {
                return transaction(operation)
            } catch (exception: SQLException) {
                if (attempt >= 3 || !exception.isDb2Deadlock()) {
                    throw exception
                }
                val delayMillis = ThreadLocalRandom.current().nextLong(100L, 201L) * attempt
                logger.warn(exception) { "Db2-deadlock. Transaksjonen forsøkes på nytt etter ${delayMillis}ms (forsøk ${attempt + 1}/3)" }
                try {
                    Thread.sleep(delayMillis)
                } catch (interrupted: InterruptedException) {
                    Thread.currentThread().interrupt()
                    throw interrupted
                }
                attempt++
            }
        }
    }

    private fun SQLException.isDb2Deadlock(): Boolean {
        val exceptions = ArrayDeque<Throwable>()
        val visited = mutableSetOf<Throwable>()
        exceptions.add(this)
        while (exceptions.isNotEmpty()) {
            val exception = exceptions.removeFirst()
            if (!visited.add(exception)) continue
            if (exception is SQLException) {
                if ((exception.errorCode == -911 || exception.errorCode == -913) && exception.message.orEmpty().contains("00C90088", ignoreCase = true)) {
                    return true
                }
                exception.nextException?.let { exceptions.add(it) }
            }
            exception.cause?.let { exceptions.add(it) }
        }
        return false
    }
}
