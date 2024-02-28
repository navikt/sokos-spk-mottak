package no.nav.sokos.spk.mottak.database

import no.nav.sokos.spk.mottak.config.logger
import java.math.BigDecimal
import java.sql.Connection
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.time.LocalDate
import java.time.LocalDateTime

object RepositoryExtensions {

    inline fun <R> Connection.useAndHandleErrors(block: (Connection) -> R): R {
        try {
            use {
                return block(this)
            }
        } catch (ex: SQLException) {
            throw ex
        }
    }

    inline fun <reified T : Any?> ResultSet.getColumn(
        columnLabel: String,
        transform: (T) -> T = { it },
    ): T {
        val columnValue = when (T::class) {
            Int::class -> getInt(columnLabel)
            Long::class -> getLong(columnLabel)
            Char::class -> getString(columnLabel)?.get(0)
            Double::class -> getDouble(columnLabel)
            String::class -> getString(columnLabel)?.trim()
            Boolean::class -> getBoolean(columnLabel)
            BigDecimal::class -> getBigDecimal(columnLabel)
            LocalDate::class -> getDate(columnLabel)?.toLocalDate()
            LocalDateTime::class -> getTimestamp(columnLabel)?.toLocalDateTime()

            else -> {
                logger.error("Kunne ikke mappe fra resultatsett til datafelt av type ${T::class.simpleName}")
                throw SQLException("Kunne ikke mappe fra resultatsett til datafelt av type ${T::class.simpleName}") // TODO Feilhåndtering
            }
        }

        if (null !is T && columnValue == null) {
            logger.error { "Påkrevet kolonne '$columnLabel' er null" }
            throw SQLException("Påkrevet kolonne '$columnLabel' er null") // TODO Feilhåndtering
        }

        return transform(columnValue as T)
    }

    fun interface Parameter {
        fun addToPreparedStatement(sp: PreparedStatement, index: Int)
    }

    fun param(value: String?) = Parameter { sp: PreparedStatement, index: Int -> sp.setString(index, value) }

    fun param(value: Int) = Parameter { sp: PreparedStatement, index: Int -> sp.setInt(index, value) }

    fun PreparedStatement.withParameters(vararg parameters: Parameter?) = apply {
        var index = 1; parameters.forEach { it?.addToPreparedStatement(this, index++) }
    }

    fun <T> ResultSet.toList(mapper: ResultSet.() -> T) = mutableListOf<T>().apply {
        while (next()) {
            add(mapper())
        }
    }
}