package no.nav.sokos.spk.mottak.config

import com.ibm.db2.jcc.DB2BaseDataSource
import com.ibm.db2.jcc.DB2SimpleDataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using

object DatabaseConfig {
    fun hikariDataSource() = HikariDataSource(createHikariConfig())

    private fun createHikariConfig(): HikariConfig {
        val db2DatabaseConfig: PropertiesConfig.Db2DatabaseConfig = PropertiesConfig.Db2DatabaseConfig()
        return HikariConfig().apply {
            maximumPoolSize = 10
            poolName = "HikariPool-SOKOS-SPK-MOTTAK"
            connectionTestQuery = "select 1 from sysibm.sysdummy1"
            dataSource = DB2SimpleDataSource().apply {
                driverType = 4
                enableNamedParameterMarkers = DB2BaseDataSource.YES
                databaseName = db2DatabaseConfig.name
                serverName = db2DatabaseConfig.host
                portNumber = db2DatabaseConfig.port.toInt()
                currentSchema = db2DatabaseConfig.schema
                connectionTimeout = 1000
                commandTimeout = 10000
                user = db2DatabaseConfig.username
                setPassword(db2DatabaseConfig.password)
            }
        }
    }

    fun <A> transaction(operation: (TransactionalSession) -> A): A {
        val dataSource = DatabaseConfig.hikariDataSource()
        return using(sessionOf(dataSource, returnGeneratedKey = true)) { session ->
            session.transaction {
                operation(it)
            }
        }
    }
}