package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.BEHANDLET_JA
import no.nav.sokos.spk.mottak.domain.FNR_ENDRET
import no.nav.sokos.spk.mottak.domain.FNR_IKKE_ENDRET
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPR
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY_EKSIST
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.TransaksjonTilstand
import no.nav.sokos.spk.mottak.domain.isTransaksjonStatusOk
import no.nav.sokos.spk.mottak.listener.Db2Listener
import java.time.LocalDate

class TransaksjonstolkningTest : BehaviorSpec({
    extensions(listOf(Db2Listener))

    val validateTransaksjonService = ValidateTransaksjonService(Db2Listener.dataSource)

    Given("det finnes en innTransaksjon med et fnr som ikke eksisterer i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_ny_fnr.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med et eksisterende fnr som har blitt endret i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_fnr_endret.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST og FNR_ENDRET satt") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY_EKSIST, FNR_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med et eksisterende fnr som historisk har hatt en annen verdi i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_fnr_eksist_med_annen_verdi.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST og FNR_ENDRET ikke satt") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY_EKSIST, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med belopstype utbetaling hvor det eksisterer bare belopstype trekk for samme person i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_ulik_belopstype_historikk.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med belopstype utbetaling hvor det eksisterer samme belopstype for samme person i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_samme_belopstype_historikk.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY_EKSIST, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med et eksisterende fnr men for en annen anviser i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_ulik_anviser_historikk.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med eksisterende fnr og en art som tilhører samme fagområde som eksisterende art for personen i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_samme_anviser_historikk.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY_EKSIST, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med eksisterende fnr og en art som tilhører et annet fagområde enn eksisterende art for personen i T_TRANSAKSJON men med en annen anviser") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_ulik_fagomrade_til_eksisterende_transaskjon.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med eksisterende fnr og en art som er ulik en historisk art som tilhører et annet fagområde for personen i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_samme_fagomrade_til_eksisterende_transaskjon.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY_EKSIST, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes 2 innTransaksjoner med samme fnr hvor art tilhører ulike fagområder og personen eksisterer ikke i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_ny_person_med_2_art_i_ulike_fagomraader.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 2
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes to transaksjoner med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 2
                innTransaksjonList.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes 2 innTransaksjoner med samme fnr hvor art tilhører samme fagområde og personen eksisterer ikke i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_ny_person_med_2_art_i_like_fagomraader.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 2
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 2
                innTransaksjonList.forEachIndexed { index, innTransaksjon ->
                    val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    val tolkning =
                        when (index) {
                            0 -> TRANS_TOLKNING_NY
                            else -> TRANS_TOLKNING_NY_EKSIST
                        }
                    verifyTransaksjon(transaksjon, innTransaksjon, tolkning, FNR_IKKE_ENDRET)

                    val transaksjonTilstand = Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes 4 innTransaksjoner med samme fnr hvor det er 2 ulike art i hvert fagområde og personen eksisterer ikke i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_ny_person_med_4_art_i_2_ulike_fagomraader.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 4
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes to transaksjoner med tolkning NY to transaksjoner med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 4
                innTransaksjonList.forEachIndexed { index, innTransaksjon ->
                    val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    val tolkning =
                        when (index) {
                            0, 2 -> TRANS_TOLKNING_NY
                            else -> TRANS_TOLKNING_NY_EKSIST
                        }
                    verifyTransaksjon(transaksjon, innTransaksjon, tolkning, FNR_IKKE_ENDRET)

                    val transaksjonTilstand = Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes 2 innTransaksjoner med samme fnr hvor 1 har art i ikke eksisterende fagområde og personen eksisterer i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_person_med_2_art_og_1_nytt_fagomraade.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 2
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes to transaksjoner, en med tolkning NY og en med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 2
                innTransaksjonList.forEachIndexed { index, innTransaksjon ->
                    val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    val tolkning =
                        when (index) {
                            0 -> TRANS_TOLKNING_NY
                            else -> TRANS_TOLKNING_NY_EKSIST
                        }
                    verifyTransaksjon(transaksjon, innTransaksjon, tolkning, FNR_IKKE_ENDRET)

                    val transaksjonTilstand = Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given(
        "det finnes 4 innTransaksjoner med 4 ulike fnr hvor en har en ny art tilhørende et nytt fagområde, en har en ny art tilhørende et eksisterende fagområde, " +
            "en har en eksisterende art, og en har ingen tidligere art i T_TRANSAKSJON",
    ) {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(TestHelper.readFromResource("/database/validering/innTransaksjon_med_4_ulike_personer_med_1nyArtNyttFagomraade_1nyArtEksisterendeFagomraade_1eksisterendeArt_1nyPerson.sql")),
            )
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 4
        When("det valideres") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes 2 transaksjoner med tolkning NY_EKSIST og 2 transaksjoner med tolkning TRANS_TOLKNING_NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 4
                innTransaksjonList.forEachIndexed { index, innTransaksjon ->
                    val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    val tolkning =
                        when (index) {
                            0 -> TRANS_TOLKNING_NY
                            1, 2 -> TRANS_TOLKNING_NY_EKSIST
                            else -> TRANS_TOLKNING_NY
                        }
                    verifyTransaksjon(transaksjon, innTransaksjon, tolkning, FNR_IKKE_ENDRET)

                    val transaksjonTilstand = Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }

    Given(
        "det finnes 3 innTransaksjoner med 3 ulike fnr hvor en har en ny art tilhørende et nytt fagområde og endret fnr, en har en ny art tilhørende et eksisterende fagområde og endret fnr, " +
            "og en har en eksisterende art og endret fnr ",
    ) {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    TestHelper.readFromResource(
                        "/database/validering/innTransaksjon_med_3_ulike_personer_med_1nyArtNyttFagomraadeFnrEndret_1nyArtEksisterendeFagomraadeFnrEndret_1eksisterendeArtFnrEndret.sql",
                    ),
                ),
            )
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 3
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST og FNR_ENDRET satt og 2 transaksjoner med tolkning TRANS_TOLKNING_NY_EKSIST og FNR_ENDRET satt") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { it.isTransaksjonStatusOk() }.size shouldBe 3
                innTransaksjonList.forEachIndexed { index, innTransaksjon ->
                    val transaksjon = Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    val tolkning =
                        when (index) {
                            0 -> TRANS_TOLKNING_NY
                            else -> TRANS_TOLKNING_NY_EKSIST
                        }
                    verifyTransaksjon(transaksjon, innTransaksjon, tolkning, FNR_ENDRET)

                    val transaksjonTilstand = Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }
})

private fun verifyTransaksjonTilstand(
    transaksjonTilstand: TransaksjonTilstand,
    innTransaksjon: InnTransaksjon,
) {
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

private fun verifyTransaksjon(
    transaksjon: Transaksjon,
    innTransaksjon: InnTransaksjon,
    tolkning: String,
    fnrEndret: Char,
    transaksjonType: String = TRANSAKSJONSTATUS_OK,
) {
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
    transaksjon.transTolkning shouldBe tolkning
    transaksjon.sendtTilOppdrag shouldBe "0"
    transaksjon.fnrEndret shouldBe fnrEndret
    transaksjon.motId shouldBe innTransaksjon.innTransaksjonId.toString()
    transaksjon.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
    transaksjon.opprettetAv shouldBe systemId
    transaksjon.datoEndret.toLocalDate() shouldBe LocalDate.now()
    transaksjon.endretAv shouldBe systemId
    transaksjon.versjon shouldBe 1
    transaksjon.transTilstandType shouldBe TRANS_TILSTAND_OPR
    transaksjon.grad shouldBe innTransaksjon.grad
}
