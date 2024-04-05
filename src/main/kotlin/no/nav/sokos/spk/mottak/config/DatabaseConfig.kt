package no.nav.sokos.spk.mottak.config

import com.ibm.db2.jcc.DB2BaseDataSource
import com.ibm.db2.jcc.DB2SimpleDataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotliquery.TransactionalSession
import kotliquery.sessionOf
import kotliquery.using

class DatabaseConfig(
    private val hikariConfig: HikariConfig = DatabaseConfig.createHikariConfig()
) {
    companion object {
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
    }

    fun dataSource(): HikariDataSource = HikariDataSource(createHikariConfig())
}

fun <A> HikariDataSource.transaction(operation: (TransactionalSession) -> A): A {
    return using(sessionOf(this, returnGeneratedKey = true)) { session ->
        session.transaction { tx ->
            operation(tx)
        }
    }
}