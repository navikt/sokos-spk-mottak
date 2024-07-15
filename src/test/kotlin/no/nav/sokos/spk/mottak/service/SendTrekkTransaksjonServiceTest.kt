package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_TREKK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TREKK_SENDT_OK
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.JmsProducerTestService
import no.nav.sokos.spk.mottak.listener.MQListener

internal class SendTrekkTransaksjonServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, MQListener))

        val sendTrekkTransaksjonTilOppdragService: SendTrekkTransaksjonTilOppdragService by lazy {
            SendTrekkTransaksjonTilOppdragService(
                Db2Listener.dataSource,
                JmsProducerTestService(MQListener.connectionFactory),
            )
        }

        Given("det finnes trekk som skal sendes til oppdragZ") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(TestHelper.readFromResource("/database/trekk_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_TREKK, TRANS_TILSTAND_TIL_TREKK).size shouldBe 10
            When("hent trekk og send til OppdragZ") {
                sendTrekkTransaksjonTilOppdragService.hentTrekkTransaksjonOgSendTilOppdrag()
                Then("skal alle transaksjoner blir oppdatert med status TSO (Trekk Send OK)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000402)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_OK }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_TREKK_SENDT_OK }
                }
            }
        }
    })
