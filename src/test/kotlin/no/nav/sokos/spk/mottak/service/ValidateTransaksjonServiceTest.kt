package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.AvvikTransaksjon
import no.nav.sokos.spk.mottak.domain.BEHANDLET_JA
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPR
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.TransaksjonTilstand
import no.nav.sokos.spk.mottak.domain.isTransaksjonStatusOK
import no.nav.sokos.spk.mottak.listener.Db2Listener


class ValidateTransaksjonServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener))

    val validateTransaksjonService = ValidateTransaksjonService(Db2Listener.dataSource)

    Given("det finnes innTransaksjoner som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/person.sql")))
            session.update(queryOf(readFromResource("/database/innTransaksjon.sql")))
            session.update(queryOf(readFromResource("/database/innTransaksjon_avvik.sql")))
        }

        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 15

        When("skal det validere ") {
            validateTransaksjonService.validateInnTransaksjon()

            Then("skal det opprettes transaksjon og avvik transaksjon") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
                innTransaksjonMap[true]!!.size shouldBe 10
                innTransaksjonMap[false]!!.size shouldBe 5

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
                    val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon)

                    val transaksjonTilstand = Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon = Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjon(avvikTransaksjon, innTransaksjon)
                }
            }
        }
    }
})

private fun verifyTransaksjonTilstand(transaksjonTilstand: TransaksjonTilstand, innTransaksjon: InnTransaksjon) {
    val systemId = PropertiesConfig.Configuration().naisAppName

    transaksjonTilstand.transaksjonId shouldBe innTransaksjon.innTransaksjonId
    transaksjonTilstand.transaksjonTilstandId shouldNotBe null
    transaksjonTilstand.transaksjonTilstandType shouldBe TRANS_TILSTAND_OPR
    transaksjonTilstand.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
    transaksjonTilstand.opprettetAv shouldBe systemId
    transaksjonTilstand.datoEndret.toLocalDate() shouldBe LocalDate.now()
    transaksjonTilstand.endretAv shouldBe systemId
    transaksjonTilstand.versjon shouldBe 1
}

private fun verifyAvvikTransaksjon(avvikTransaksjon: AvvikTransaksjon, innTransaksjon: InnTransaksjon) {
    val systemId = PropertiesConfig.Configuration().naisAppName

    avvikTransaksjon.avvikTransaksjonId shouldBe innTransaksjon.innTransaksjonId
    avvikTransaksjon.filInfoId shouldBe innTransaksjon.filInfoId
    avvikTransaksjon.transaksjonStatus shouldBe innTransaksjon.transaksjonStatus
    avvikTransaksjon.fnr shouldBe innTransaksjon.fnr
    avvikTransaksjon.belopType shouldBe innTransaksjon.belopstype
    avvikTransaksjon.art shouldBe innTransaksjon.art
    avvikTransaksjon.avsender shouldBe innTransaksjon.avsender
    avvikTransaksjon.utbetalesTil shouldBe innTransaksjon.utbetalesTil
    avvikTransaksjon.datoFom shouldBe innTransaksjon.datoFomStr
    avvikTransaksjon.datoTom shouldBe innTransaksjon.datoTomStr
    avvikTransaksjon.datoAnviser shouldBe innTransaksjon.datoAnviserStr
    avvikTransaksjon.belop shouldBe innTransaksjon.belopStr
    avvikTransaksjon.refTransId shouldBe innTransaksjon.refTransId
    avvikTransaksjon.tekstKode shouldBe innTransaksjon.tekstkode
    avvikTransaksjon.rectType shouldBe innTransaksjon.rectype
    avvikTransaksjon.transEksId shouldBe innTransaksjon.transId
    avvikTransaksjon.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
    avvikTransaksjon.opprettetAv shouldBe systemId
    avvikTransaksjon.datoEndret.toLocalDate() shouldBe LocalDate.now()
    avvikTransaksjon.endretAv shouldBe systemId
    avvikTransaksjon.versjon shouldBe 1
    avvikTransaksjon.grad shouldBe innTransaksjon.grad
}


private fun verifyTransaksjon(transaksjon: Transaksjon, innTransaksjon: InnTransaksjon, transaksjonType: String = TRANSAKSJONSTATUS_OK) {
    val systemId = PropertiesConfig.Configuration().naisAppName

    transaksjon.transaksjonId shouldBe innTransaksjon.innTransaksjonId
    transaksjon.filInfoId shouldBe innTransaksjon.filInfoId
    transaksjon.transaksjonStatus shouldBe transaksjonType
    transaksjon.personId shouldBe innTransaksjon.personId
    transaksjon.belopstype shouldBe innTransaksjon.belopstype
    transaksjon.art shouldBe innTransaksjon.art
    transaksjon.anviser shouldBe innTransaksjon.avsender
    transaksjon.fnr shouldBe innTransaksjon.fnr
    transaksjon.utbetalesTil shouldBe innTransaksjon.utbetalesTil
    transaksjon.datoFom shouldBe innTransaksjon.datoFom
    transaksjon.datoTom shouldBe innTransaksjon.datoTom
    transaksjon.datoAnviser shouldBe innTransaksjon.datoAnviser
    transaksjon.datoPersonFom shouldBe LocalDate.of(1900, 1, 1)
    transaksjon.datoReakFom shouldBe LocalDate.of(1900, 1, 1)
    transaksjon.belop shouldBe innTransaksjon.belop
    transaksjon.refTransId shouldBe innTransaksjon.refTransId
    transaksjon.tekstkode shouldBe innTransaksjon.tekstkode
    transaksjon.rectype shouldBe innTransaksjon.rectype
    transaksjon.transEksId shouldBe innTransaksjon.transId
    transaksjon.transTolkning shouldBe TRANS_TOLKNING_NY
    transaksjon.sendtTilOppdrag shouldBe "0"
    transaksjon.fnrEndret shouldBe '0'
    transaksjon.motId shouldBe null
    transaksjon.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
    transaksjon.opprettetAv shouldBe systemId
    transaksjon.datoEndret.toLocalDate() shouldBe LocalDate.now()
    transaksjon.endretAv shouldBe systemId
    transaksjon.versjon shouldBe 1
    transaksjon.transTilstandType shouldBe TRANS_TILSTAND_OPR
    transaksjon.grad shouldBe innTransaksjon.grad
}