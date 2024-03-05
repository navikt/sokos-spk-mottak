package no.nav.sokos.spk.mottak.database


import no.nav.sokos.spk.mottak.config.logger
import no.nav.sokos.spk.mottak.database.RepositoryExtensions.Parameter
import java.math.BigDecimal
import java.sql.Connection
import java.sql.Date
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.LocalDate
import java.time.LocalDateTime

object RepositoryExtensions {

    var batchsize: Int = 5000
    var antallTransaksjoner: Int = 0
    inline fun <R> Connection.useAndHandleErrors(block: (Connection) -> R): R {
        try {
            use {
                return block(this)
            }
        } catch (ex: SQLException) {
            rollback()
            // TODO: log exception
            throw ex
        }
    }

    inline fun <PreparedStatement> Connection.insertTransaction(block: (Connection) -> PreparedStatement): PreparedStatement {
        try {
            var st: PreparedStatement = block(this)
            if (antallTransaksjoner++.equals(batchsize)) {
                antallTransaksjoner = 0
            }
        } catch (ex: SQLException) {
            rollback()
            // TODO: log exception
            throw ex
        } finally {
            rollback()
            // TODO: log exception
            throw Exception()
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

    fun param(value: BigDecimal) =
        Parameter { statement: PreparedStatement, index: Int -> statement.setBigDecimal(index, value) }

    fun param(value: LocalDate) =
        Parameter { statement: PreparedStatement, index: Int -> statement.setDate(index, Date.valueOf(value)) }

    fun param(value: LocalDateTime) =
        Parameter { statement: PreparedStatement, index: Int ->
            statement.setTimestamp(
                index,
                Timestamp.valueOf(value)
            )
        }

    fun PreparedStatement.withParameters(vararg parameters: Parameter?) = apply {
        var index = 1; parameters.forEach { it?.addToPreparedStatement(this, index++) }
    }

    fun <T> ResultSet.toList(mapper: ResultSet.() -> T) = mutableListOf<T>().apply {
        while (next()) {
            add(mapper())
        }
    }
}