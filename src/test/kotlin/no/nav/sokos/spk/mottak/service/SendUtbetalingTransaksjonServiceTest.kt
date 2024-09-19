package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.annotation.Ignored
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_FEIL
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPDRAG_SENDT_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_TIL_OPPDRAG
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.MQListener
import no.nav.sokos.spk.mottak.repository.OutboxRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonRepository
import no.nav.sokos.spk.mottak.repository.TransaksjonTilstandRepository
import java.sql.SQLException

@Ignored
internal class SendUtbetalingTransaksjonServiceTest :
    BehaviorSpec({
        extensions(listOf(Db2Listener, MQListener))

        Given("det finnes utbetalinger som skal sendes til oppdragZ") {
            val utbetalingTransaksjonTilOppdragService: SendUtbetalingTransaksjonToOppdragServiceV2 by lazy {
                SendUtbetalingTransaksjonToOppdragServiceV2(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    Db2Listener.outboxRepository,
                )
            }

            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG).size shouldBe 10
            When("hent utbetalinger og send til OppdragZ") {
                utbetalingTransaksjonTilOppdragService.fetchUtbetalingTransaksjonAndSendToOppdrag()
                Then("skal alle transaksjoner blir oppdatert med status OSO (Oppdrag Sendt OK)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000002)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_OK }
                }
            }
        }

        Given("det finnes utbetalinger som skal sendes til oppdragZ med MQ server som er nede") {
            val utbetalingTransaksjonTilOppdragService =
                SendUtbetalingTransaksjonToOppdragServiceV2(
                    Db2Listener.dataSource,
                    Db2Listener.transaksjonRepository,
                    Db2Listener.transaksjonTilstandRepository,
                    Db2Listener.outboxRepository,
                )

            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/utbetaling_transaksjon.sql")))
            }
            Db2Listener.transaksjonRepository.findAllByBelopstypeAndByTransaksjonTilstand(BELOPTYPE_TIL_OPPDRAG, TRANS_TILSTAND_TIL_OPPDRAG).size shouldBe 10
            When("hent utbetalinger og send til OppdragZ") {
                utbetalingTransaksjonTilOppdragService.fetchUtbetalingTransaksjonAndSendToOppdrag()
                Then("skal alle transaksjoner blir oppdatert med status OSF (Oppdrag Sendt Feil)") {
                    val transaksjonList = Db2Listener.transaksjonRepository.findAllByFilInfoId(filInfoId = 20000002)
                    transaksjonList.map { it.transTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_FEIL }

                    val transaksjonTilstandList = Db2Listener.transaksjonTilstandRepository.findAllByTransaksjonId(transaksjonList.map { it.transaksjonId!! })
                    transaksjonTilstandList.size shouldBe 10
                    transaksjonTilstandList.map { it.transaksjonTilstandType shouldBe TRANS_TILSTAND_OPPDRAG_SENDT_FEIL }
                }
            }
        }

        Given("det finnes utbetalinger som skal sendes til oppdragZ med database som er nede") {
            val dataSourceMock = mockk<HikariDataSource>()
            every { dataSourceMock.connection } throws SQLException("No database connection!")

            val utbetalingTransaksjonTilOppdragService =
                SendUtbetalingTransaksjonToOppdragServiceV2(
                    dataSourceMock,
                    mockk<TransaksjonRepository>(),
                    mockk<TransaksjonTilstandRepository>(),
                    mockk<OutboxRepository>(),
                )

            When("hent utbetalinger og send til OppdragZ") {
                val exception = shouldThrow<MottakException> { utbetalingTransaksjonTilOppdragService.fetchUtbetalingTransaksjonAndSendToOppdrag() }

                Then("skal det kaste en feilmelding og SendUtbetalingTransaksjonToOppdragServiceV2 stoppet") {
                    exception.message shouldBe "Feil under sending av utbetalingstransaksjoner til OppdragZ. Feilmelding: No database connection!"
                }
            }
        }
    })
