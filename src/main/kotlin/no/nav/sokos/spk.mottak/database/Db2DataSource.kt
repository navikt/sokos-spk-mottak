package no.nav.sokos.spk.mottak.database

import com.ibm.db2.jcc.DB2BaseDataSource
import com.ibm.db2.jcc.DB2SimpleDataSource
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection
import no.nav.sokos.spk.mottak.config.PropertiesConfig

class Db2DataSource(
    private val oppdragDatabaseConfig: PropertiesConfig.Db2DatabaseConfig = PropertiesConfig.Db2DatabaseConfig()
) {
    private val dataSource: HikariDataSource = HikariDataSource(hikariConfig())

    val connection: Connection get() = dataSource.connection

    fun close() = dataSource.close()

    private fun hikariConfig() = HikariConfig().apply {
        maximumPoolSize = 10
        isAutoCommit = true
        poolName = "HikariPool-SOKOS-SPK-MOTTAK"
        connectionTestQuery = "select 1 from sysibm.sysdummy1"
        dataSource = DB2SimpleDataSource().apply {
            driverType = 4
            enableNamedParameterMarkers = DB2BaseDataSource.YES
            databaseName = oppdragDatabaseConfig.name
            serverName = oppdragDatabaseConfig.host
            portNumber = oppdragDatabaseConfig.port.toInt()
            currentSchema = oppdragDatabaseConfig.schema
            connectionTimeout = 1000
            commandTimeout = 10000
            user = oppdragDatabaseConfig.username
            setPassword(oppdragDatabaseConfig.password)
        }
    }
}