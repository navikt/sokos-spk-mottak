package no.nav.sokos.spk.mottak.config

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import org.postgresql.ds.PGSimpleDataSource

class DatabaseTestConfigTest :
    FunSpec({
        test("Postgres test config uses the supplied port") {
            val dataSource = DatabaseTestConfig.hikariPostgresConfig("localhost", 5433).dataSource as PGSimpleDataSource

            dataSource.portNumbers.toList() shouldBe listOf(5433)
        }
    })
