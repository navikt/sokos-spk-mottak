package no.nav.sokos.spk.mottak.repository

import com.zaxxer.hikari.HikariDataSource
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

import no.nav.sokos.spk.mottak.config.DatabaseConfig
import no.nav.sokos.spk.mottak.domain.LeveAttester
import no.nav.sokos.spk.mottak.metrics.DATABASE_CALL
import no.nav.sokos.spk.mottak.metrics.Metrics

class LeveAttesterRepository(
    private val dataSource: HikariDataSource = DatabaseConfig.db2DataSource,
) {
    private val getLeveAttesterTimer = Metrics.timer(DATABASE_CALL, "LeveAttesterRepository", "getLeveAttester")

    fun getLeveAttester(datoFom: String): List<LeveAttester> =
        using(sessionOf(dataSource)) { session ->
            getLeveAttesterTimer.recordCallable {
                session.list(
                    queryOf(
                        """
                        SELECT DISTINCT FNR_FK, K_ANVISER FROM T_TRANSAKSJON 
                        WHERE K_ANVISER = 'SPK'
                        AND DATO_FOM = '$datoFom'
                        """.trimIndent(),
                    ),
                    mapToLeveAttester,
                )
            }
        }
}

private val mapToLeveAttester: (Row) -> LeveAttester = { row ->
    LeveAttester(
        fnrFk = row.string("FNR_FK"),
        kAnviser = row.string("K_ANVISER"),
    )
}
