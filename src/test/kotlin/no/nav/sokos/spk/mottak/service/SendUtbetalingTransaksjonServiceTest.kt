package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.JmsProducerTestService
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.listener.MQListener.connectionFactoryMock

internal class SendUtbetalingTransaksjonServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, MQListener))

        Given("det finnes utbetalinger som skal sendes til oppdragZ") {
            val utbetalingTransaksjonTilOppdragService: SendUtbetalingTransaksjonTilOppdragService by lazy {
                SendUtbetalingTransaksjonTilOppdragService(Db2Listener.dataSource, JmsProducerTestService(MQListener.connectionFactory))
            }

            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG).size shouldBe 10
            When("hent utbetalinger og send til OppdragZ") {
                utbetalingTransaksjonTilOppdragService.hentUtbetalingTransaksjonOgSendTilOppdrag()
                Then("skal alle transaksjoner blir oppdatert med status OSO (Oppdrag Send OK)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000002)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK }
                }
            }
        }

        Given("det finnes utbetalinger som skal sendes til oppdragZ med MQ server kaster feil") {
            val utbetalingTransaksjonTilOppdragService = SendUtbetalingTransaksjonTilOppdragService(Db2Listener.dataSource, JmsProducerTestService(connectionFactoryMock))

            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG).size shouldBe 10
            When("hent utbetalinger og send til OppdragZ") {
                utbetalingTransaksjonTilOppdragService.hentUtbetalingTransaksjonOgSendTilOppdrag()
                Then("skal alle transaksjoner blir oppdatert med status OSF (Oppdrag Send Feil)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000002)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_FEIL }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_FEIL }
                }
            }
        }
    })
