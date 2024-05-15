package no.nav.sokos.spk.mottak.listener

import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.DatabaseTestConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.FILTYPE_ANVISER
import no.nav.sokos.spk.mottak.repository.AvvikTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.FilInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.LopenummerRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository

object Db2Listener : TestListener {
    val dataSource = HikariDataSource(DatabaseTestConfig.hikariConfig())
    val lopeNummerRepository = LopenummerRepository(dataSource)
    val innTransaksjonRepository = InnTransaksjonRepository(dataSource)
    val filInfoRepository = FilInfoRepository(dataSource)
    val transaksjonRepository = TransaksjonRepository(dataSource)
    val avvikTransaksjonRepository = AvvikTransaksjonRepository(dataSource)
    val transaksjonTilstandRepository = TransaksjonTilstandRepository(dataSource)

    override suspend fun beforeSpec(spec: Spec) {
        dataSource shouldNotBe null
        lopeNummerRepository shouldNotBe null
        innTransaksjonRepository shouldNotBe null
        filInfoRepository shouldNotBe null
        transaksjonRepository shouldNotBe null
        avvikTransaksjonRepository shouldNotBe null
        transaksjonRepository shouldNotBe null

        dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/db2Script.sql")))
        }
    }

    override suspend fun afterEach(
        testCase: TestCase,
        result: TestResult,
    ) {
        resetDatabase()
    }

    private fun resetDatabase() {
        dataSource.transaction { session ->
            lopeNummerRepository.updateLopeNummer(33, FILTYPE_ANVISER, session)
            session.update(queryOf("DELETE FROM T_INN_TRANSAKSJON"))
            session.update(queryOf("DELETE FROM T_FIL_INFO"))
            session.update(queryOf("DELETE FROM T_PERSON"))
            session.update(queryOf("DELETE FROM T_INN_TRANSAKSJON"))
            session.update(queryOf("DELETE FROM T_TRANS_TILSTAND"))
            session.update(queryOf("DELETE FROM T_TRANSAKSJON"))
            session.update(queryOf("DELETE FROM T_AVV_TRANSAKSJON"))
        }
    }
}
