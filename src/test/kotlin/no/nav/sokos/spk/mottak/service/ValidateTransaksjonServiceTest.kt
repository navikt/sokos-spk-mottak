package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.AvvikTransaksjon
import no.nav.sokos.spk.mottak.domain.BEHANDLET_JA
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.BELOPTYPE_TREKK
import no.nav.sokos.spk.mottak.domain.FNR_ENDRET
import no.nav.sokos.spk.mottak.domain.FNR_IKKE_ENDRET
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPR
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY_EKSIST
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.TransaksjonTilstand
import no.nav.sokos.spk.mottak.domain.isValideringStatusIsOK
import no.nav.sokos.spk.mottak.listener.Db2Listener
import java.time.LocalDate

class ValidateTransaksjonServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener))

    val validateTransaksjonService = ValidateTransaksjonService(Db2Listener.dataSource)

    Given("det finnes innTransaksjoner som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/person.sql")))
            session.update(queryOf(readFromResource("/database/innTransaksjon.sql")))
            session.update(queryOf(readFromResource("/database/innTransaksjon_avvik.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 15
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en ok-transaksjon og en avvikstransaksjon") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 10
                innTransaksjonMap[false]!!.size shouldBe 5

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY, FNR_IKKE_ENDRET)

                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjon(avvikTransaksjon, innTransaksjon)
                }
            }
        }
    }

    Given("det finnes to innTransaksjoner som er dubletter") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/duplikat_innTransaksjoner.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 2
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 01") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 2

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "01")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som er dublett med en eksisterende avvist transaksjon") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/duplikat_innTransaksjon_med_avvist_transaksjon.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 01") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "01")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som er dublett med en eksisterende transaksjon") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/duplikat_innTransaksjon_med_transaksjon.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 01") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "01")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som har ugyldig fnr") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ugyldig_fnr.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 02") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "02")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med belopstype skattepliktig utbetaling som har ugyldig DatoFom") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_belopstype_01_ugyldig_datofom.sql")))
        }
        val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPTYPE_SKATTEPLIKTIG_UTBETALING

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonMap = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA).groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "03")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med belopstype skattepliktig utbetaling som har ugyldig DatoTom") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_belopstype_01_ugyldig_datotom.sql")))
        }
        val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPTYPE_SKATTEPLIKTIG_UTBETALING

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonMap = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA).groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "03")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med belopstype trekk som har ugyldig DatoFom") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_belopstype_03_ugyldig_datofom.sql")))
        }
        val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPTYPE_TREKK

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonMap = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA).groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "03")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med belopstype 03 som har ugyldig DatoTom") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_belopstype_03_ugyldig_datotom.sql")))
        }
        val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPTYPE_TREKK

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonMap = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA).groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "03")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som har ugyldig belopstype") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ugyldig_belopstype.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 04") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "04")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som har ugyldig art") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ugyldig_art.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 05") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "05")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som har ugyldig anviser dato") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ugyldig_anviser_dato.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 09") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "09")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som har ugyldig beløp") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ugyldig_belop.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 10") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "10")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som har ugyldig kombinasjon av art og belopstype") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_ugyldig_kombinasjon_av_art_og_belopstype.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 11") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true] shouldBe null
                innTransaksjonMap[false]!!.size shouldBe 1

                innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "11")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon med et fnr som ikke eksisterer i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ny_fnr.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 1
                innTransaksjonMap[false] shouldBe null

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_fnr_endret.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST og FNR_ENDRET satt") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 1
                innTransaksjonMap[false] shouldBe null

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_fnr_eksist_med_annen_verdi.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST og FNR_ENDRET ikke satt") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 1
                innTransaksjonMap[false] shouldBe null

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ulik_belopstype_historikk.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 1
                innTransaksjonMap[false] shouldBe null

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_samme_belopstype_historikk.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 1
                innTransaksjonMap[false] shouldBe null

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ulik_anviser_historikk.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 1
                innTransaksjonMap[false] shouldBe null

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_samme_anviser_historikk.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 1
                innTransaksjonMap[false] shouldBe null

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ulik_fagomrade_til_eksisterende_transaskjon.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 1
                innTransaksjonMap[false] shouldBe null

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
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

    Given(
        "det finnes en innTransaksjon med eksisterende fnr og en art som er ulik en historisk art som tilhører et annet fagområde for personen i T_TRANSAKSJON",
    ) {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_samme_fagomrade_til_eksisterende_transaskjon.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isValideringStatusIsOK() }
                innTransaksjonMap[true]!!.size shouldBe 1
                innTransaksjonMap[false] shouldBe null

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
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

private fun verifyAvvikTransaksjonMedValideringsfeil(
    avvikTransaksjon: AvvikTransaksjon,
    innTransaksjon: InnTransaksjon,
    valideringsfeil: String,
) {
    return verifyAvvikTransaksjon(avvikTransaksjon, innTransaksjon, valideringsfeil)
}

private fun verifyAvvikTransaksjon(
    avvikTransaksjon: AvvikTransaksjon,
    innTransaksjon: InnTransaksjon,
    valideringsfeil: String? = null,
) {
    val systemId = PropertiesConfig.Configuration().naisAppName

    avvikTransaksjon.avvikTransaksjonId shouldBe innTransaksjon.innTransaksjonId
    avvikTransaksjon.filInfoId shouldBe innTransaksjon.filInfoId
    when {
        valideringsfeil != null -> avvikTransaksjon.transaksjonStatus shouldBe valideringsfeil
        else -> avvikTransaksjon.transaksjonStatus shouldBe innTransaksjon.transaksjonStatus
    }
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
