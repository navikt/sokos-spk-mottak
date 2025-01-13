package no.nav.sokos.spk.mottak.listener

import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.TestListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldNotBe
import io.mockk.spyk
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.DatabaseTestConfig
import no.nav.sokos.spk.mottak.domain.FILTYPE_ANVISER
import no.nav.sokos.spk.mottak.repository.AvvikTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.FilInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.LopenummerRepository
import no.nav.sokos.spk.mottak.repository.PersonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

object Db2Listener : TestListener {
    val dataSource = HikariDataSource(DatabaseTestConfig.hikariConfig())
    val lopeNummerRepository = spyk(LopenummerRepository(dataSource))
    val innTransaksjonRepository = spyk(InnTransaksjonRepository(dataSource))
    val filInfoRepository = spyk(FilInfoRepository(dataSource))
    val transaksjonRepository = spyk(TransaksjonRepository(dataSource))
    val avvikTransaksjonRepository = spyk(AvvikTransaksjonRepository(dataSource))
    val transaksjonTilstandRepository = spyk(TransaksjonTilstandRepository(dataSource))
    val personRepository = spyk(PersonRepository(dataSource))

    override suspend fun beforeSpec(spec: Spec) {
        dataSource shouldNotBe null
        lopeNummerRepository shouldNotBe null
        innTransaksjonRepository shouldNotBe null
        filInfoRepository shouldNotBe null
        transaksjonRepository shouldNotBe null
        avvikTransaksjonRepository shouldNotBe null
        transaksjonTilstandRepository shouldNotBe null
        personRepository shouldNotBe null

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
            lopeNummerRepository.updateLopeNummer("000033", FILTYPE_ANVISER, session)
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
