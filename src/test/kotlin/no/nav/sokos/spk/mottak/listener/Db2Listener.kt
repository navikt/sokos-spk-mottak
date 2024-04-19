package no.nav.sokos.spk.mottak.listener

import com.zaxxer.hikari.HikariDataSource
import io.kotest.core.listeners.AfterSpecListener
import io.kotest.core.listeners.BeforeSpecListener
import io.kotest.core.spec.Spec
import io.kotest.matchers.shouldNotBe
import no.nav.sokos.spk.mottak.config.DatabaseTestConfig
import no.nav.sokos.spk.mottak.repository.FileInfoRepository
import no.nav.sokos.spk.mottak.repository.InnTransaksjonRepository
import no.nav.sokos.spk.mottak.repository.LopenummerRepository

object Db2Listener : BeforeSpecListener, AfterSpecListener {
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
        dataSource.getConnection().createStatement().execute("DROP TABLE IF EXISTS T_LOPENR")
        dataSource.getConnection().createStatement().execute("DROP TABLE IF EXISTS  T_FIL_INFO")
        dataSource.getConnection().createStatement().execute("DROP TABLE IF EXISTS  T_INN_TRANSAKSJON")
        dataSource.getConnection().createStatement().execute("SHUTDOWN");
        dataSource.close()
    }
}