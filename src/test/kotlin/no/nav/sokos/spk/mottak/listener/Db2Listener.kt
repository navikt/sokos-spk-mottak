package no.nav.sokos.spk.mottak.listener

import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.AfterEachListener
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.core.test.TestResult
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.config.DatabaseTestConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.FILETYPE_ANVISER
import no.nav.sokos.spk.mottak.repository.FileInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.LopenummerRepository

object Db2Listener : BeforeSpecListener, AfterSpecListener, AfterEachListener {
    val dataSource = HikariDataSource(DatabaseTestConfig().hikariConfig())
    val lopenummerRepository = LopenummerRepository(dataSource)
    val innTransaksjonRepository = InnTransaksjonRepository(dataSource)
    val fileInfoRepository = FileInfoRepository(dataSource)

    override suspend fun beforeSpec(spec: Spec) {
        dataSource shouldNotBe null
        lopenummerRepository shouldNotBe null
        innTransaksjonRepository shouldNotBe null
        fileInfoRepository shouldNotBe null
    }

    override suspend fun afterSpec(spec: Spec) {
        dataSource.close()
    }

    override suspend fun afterEach(testCase: TestCase, result: TestResult) {
        resetDatabase()
    }

    private fun resetDatabase() {
        dataSource.transaction { session ->
            lopenummerRepository.updateLopenummer(33, FILETYPE_ANVISER, session)
            session.update(queryOf("DELETE FROM T_INN_TRANSAKSJON"))
            session.update(queryOf("DELETE FROM T_FIL_INFO"))
        }
    }
}