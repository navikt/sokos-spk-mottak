package no.nav.sokos.spk.mottak.config

import com.zaxxer.hikari.HikariConfig

class DatabaseTestConfig {
    fun hikariConfig() =
        HikariConfig().apply {
            jdbcUrl = "jdbc:h2:mem:test_mottak;MODE=DB2;DB_CLOSE_DELAY=-1;"
            driverClassName = "org.h2.Driver"
            maximumPoolSize = 10
            validate()
        }
}
