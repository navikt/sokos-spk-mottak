package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import kotliquery.queryOf

import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

internal class LeveAttesterServiceTest :
    FunSpec({

        extensions(listOf(Db2Listener))

        val leveAttestService: LeveAttestService by lazy {
            LeveAttestService(
                leveAttestRepository = Db2Listener.leveAttestRepository,
            )
        }

        test("getLeveAttester should return empty list") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            }

            val result = leveAttestService.getLeveAttester("2009-01-01")
            result.size shouldBe 10
        }
    })
