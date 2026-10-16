package no.nav.sokos.spk.mottak.util

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.PreparedStatement
import javax.sql.DataSource

import kotlin.reflect.full.memberProperties

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using

object SQLUtils {
    fun <A> HikariDataSource.transaction(operation: (TransactionalSession) -> A): A = transaction(null, operation)

    inline fun <reified T : Any> Row.optionalOrNull(columnLabel: String): T? =
        runCatching {
            this.any(columnLabel) as? T
        }.getOrNull()

    inline fun <reified T : Any> T.asMap(): Map<String, Any?> {
        val props = T::class.memberProperties.associateBy { it.name }
        return props.keys.associateWith { props[it]?.get(this) }
    }

    fun <A> DataSource.transaction(
        queryTimeout: (() -> Int)? = null,
        operation: (TransactionalSession) -> A,
    ): A =
        using(sessionOf(queryTimeout?.let { withQueryTimeout(this, it) } ?: this, returnGeneratedKey = true)) { session ->
            var operationFailure: Exception? = null
            try {
                session.transaction { tx ->
                    try {
                        operation(tx)
                    } catch (feil: Exception) {
                        operationFailure = feil
                        throw feil
                    }
                }
            } catch (feil: Exception) {
                val opprinnelig = operationFailure
                if (opprinnelig != null && opprinnelig !== feil) {
                    opprinnelig.addSuppressed(feil)
                    throw opprinnelig
                }
                throw feil
            }
        }

    private fun withQueryTimeout(
        dataSource: DataSource,
        queryTimeout: () -> Int,
    ): DataSource =
        object : DataSource by dataSource {
            override fun getConnection(): Connection = dataSource.getConnection().withQueryTimeout(queryTimeout)

            override fun getConnection(
                username: String,
                password: String,
            ): Connection = dataSource.getConnection(username, password).withQueryTimeout(queryTimeout)
        }

    private fun Connection.withQueryTimeout(queryTimeout: () -> Int): Connection {
        val connection = this
        return Proxy.newProxyInstance(Connection::class.java.classLoader, arrayOf(Connection::class.java)) { _, method, arguments ->
            try {
                val result = method.invoke(connection, *(arguments ?: emptyArray()))
                if (method.name.startsWith("prepareStatement") && result is PreparedStatement) {
                    result.withQueryTimeout(queryTimeout)
                } else {
                    result
                }
            } catch (feil: InvocationTargetException) {
                throw feil.targetException
            }
        } as Connection
    }

    private fun PreparedStatement.withQueryTimeout(timeoutProvider: () -> Int): PreparedStatement {
        val statement = this
        return Proxy.newProxyInstance(PreparedStatement::class.java.classLoader, arrayOf(PreparedStatement::class.java)) { _, method, arguments ->
            try {
                if (method.name.startsWith("execute")) {
                    val timeout = timeoutProvider()
                    require(timeout > 0)
                    statement.queryTimeout = timeout
                }
                method.invoke(statement, *(arguments ?: emptyArray()))
            } catch (feil: InvocationTargetException) {
                throw feil.targetException
            }
        } as PreparedStatement
    }
}
