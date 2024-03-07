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

    private var batchsize: Int = 4000
    private var antallTransaksjoner: Int = 0
    inline fun <R> Connection.useAndHandleErrors(block: (Connection) -> R): R {
        try {
            use {
                return block(this)
            }
        } catch (ex: SQLException) { // håndterer ikke rollback
            logger.error("Feiler sql query: ${ex.message}")
            throw ex
        }
    }

    fun PreparedStatement.executeBatchConditional(conn: Connection) = apply {
        try {
            if (antallTransaksjoner++ % batchsize == 0) {
                executeBatch()
                close()
                conn.commit()
            }
        } catch (ex: SQLException) {
            logger.error("Feiler ved batch insert: ${ex.message}")
            conn.rollback()
            throw ex
        } finally {
            conn.close();
        }
    }

    fun PreparedStatement.executeBatchUnConditional(conn: Connection) = apply {
        try {
            executeBatch()
            close()
            conn.commit()
        } catch (ex: SQLException) {
            logger.error("Feiler ved batch insert: ${ex.message}")
            conn.rollback()
            throw ex
        } finally {
            conn.close();
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