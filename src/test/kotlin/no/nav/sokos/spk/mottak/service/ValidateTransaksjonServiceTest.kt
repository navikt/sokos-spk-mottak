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
import no.nav.sokos.spk.mottak.domain.isTransaksjonStatusOK
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 15
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en ok-transaksjon og en avvikstransaksjon") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 2
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 01") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 01") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 01") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_med_ugyldig_fnr.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 02") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_med_belopstype_01_ugyldig_datofom.sql")))
        }
        val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPTYPE_SKATTEPLIKTIG_UTBETALING

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonMap = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA).groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_med_belopstype_01_ugyldig_datotom.sql")))
        }
        val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPTYPE_SKATTEPLIKTIG_UTBETALING

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonMap = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA).groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_med_belopstype_03_ugyldig_datofom.sql")))
        }
        val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPTYPE_TREKK

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonMap = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA).groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_med_belopstype_03_ugyldig_datotom.sql")))
        }
        val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPTYPE_TREKK

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonMap = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA).groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_med_ugyldig_belopstype.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 04") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_med_ugyldig_art.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 05") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_med_ugyldig_anviser_dato.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 09") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_med_ugyldig_belop.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 10") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
            session.update(queryOf(readFromResource("/database/validering/innTransaksjoner_ugyldig_kombinasjon_av_art_og_belopstype.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 11") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST og FNR_ENDRET satt") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST og FNR_ENDRET ikke satt") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        var personId: Long?
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, " +
                        "REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, " +
                        "GRAD, GRAD_STR) " +
                        "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', " +
                        "'2023-06-30', " +
                        "'2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)",
                ),
            )
            personId =
                session.updateAndReturnGeneratedKey(
                    queryOf(
                        "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-31 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)",
                    ),
                )
            session.update(
                queryOf(
                    "insert into T_TRANSAKSJON (TRANSAKSJON_ID, TRANS_TILSTAND_ID, FIL_INFO_ID, K_TRANSAKSJON_S, PERSON_ID, K_BELOP_T, K_ART, K_ANVISER, FNR_FK, UTBETALES_TIL, OS_ID_FK, " +
                        "OS_LINJE_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, DATO_PERSON_FOM, DATO_REAK_FOM, BELOP, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_EKS_ID_FK, K_TRANS_TOLKNING, " +
                        "SENDT_TIL_OPPDRAG, TREKKVEDTAK_ID_FK, FNR_ENDRET, MOT_ID, OS_STATUS, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, K_TREKKANSVAR, K_TRANS_TILST_T, GRAD) " +
                        "values (98000000, null, 20000816, '00', " + personId + ", '02', 'UFE', 'OK', '66043800214', null, null, null, '2023-05-01', '2023-05-31', '2023-04-25', '1900-01-01', " +
                        "'1900-01-01', 51700, null, null, '02', '999999999', 'NY', '0', null, '0', '1', null, '2024-04-24 08:45:08.998930','sokos-spk-mottak', '2024-04-24 08:45:08.999190', " +
                        "'sokos-spk-mottak', 1, '4819', 'ORO', null)",
                ),
            )
            session.update(
                queryOf(
                    "insert into T_TRANSAKSJON (TRANSAKSJON_ID, TRANS_TILSTAND_ID, FIL_INFO_ID, K_TRANSAKSJON_S, PERSON_ID, K_BELOP_T, K_ART, K_ANVISER, FNR_FK, UTBETALES_TIL, OS_ID_FK, " +
                        "OS_LINJE_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, DATO_PERSON_FOM, DATO_REAK_FOM, BELOP, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_EKS_ID_FK, K_TRANS_TOLKNING, " +
                        "SENDT_TIL_OPPDRAG, TREKKVEDTAK_ID_FK, FNR_ENDRET, MOT_ID, OS_STATUS, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, K_TREKKANSVAR, K_TRANS_TILST_T, GRAD) " +
                        "values (99000000, null, 20000816, '00', " + personId + ", '01', 'UFT', 'OK', '66043800214', null, null, null, '2023-07-01', '2023-07-31', '2023-04-25', '1900-01-01', " +
                        "'1900-01-01', 51700, null, null, '02', '6666666666', 'NY', '0', null, '0', '1', null, '2024-04-24 08:45:08.998930','sokos-spk-mottak', '2024-04-24 08:45:08.999190', " +
                        "'sokos-spk-mottak', 1, '4819', 'ORO', null)",
                ),
            )
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
        var personId: Long?
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, " +
                        "REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, " +
                        "GRAD, GRAD_STR) " +
                        "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', " +
                        "'2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)",
                ),
            )
            personId =
                session.updateAndReturnGeneratedKey(
                    queryOf(
                        "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-31 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)",
                    ),
                )
            session.update(
                queryOf(
                    "insert into T_TRANSAKSJON (TRANSAKSJON_ID, TRANS_TILSTAND_ID, FIL_INFO_ID, K_TRANSAKSJON_S, PERSON_ID, K_BELOP_T, K_ART, K_ANVISER, FNR_FK, UTBETALES_TIL, OS_ID_FK, " +
                        "OS_LINJE_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, DATO_PERSON_FOM, DATO_REAK_FOM, BELOP, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_EKS_ID_FK, K_TRANS_TOLKNING, " +
                        "SENDT_TIL_OPPDRAG, TREKKVEDTAK_ID_FK, FNR_ENDRET, MOT_ID, OS_STATUS, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, K_TREKKANSVAR, K_TRANS_TILST_T, GRAD) " +
                        "values (98000000, null, 20000816, '00', " + personId + ", '02', 'UFT', 'SPK', '66043800214', null, null, null, '2023-04-01', '2023-04-30', '2023-04-25', '1900-01-01', " +
                        "'1900-01-01', 51700, null, null, '02', '999999999', 'NY', '0', null, '0', '1', null, '2024-04-24 08:45:08.998930','sokos-spk-mottak', '2024-04-24 08:45:08.999190', " +
                        "'sokos-spk-mottak', 1, '4819', 'ORO', null)",
                ),
            )
            session.update(
                queryOf(
                    "insert into T_TRANSAKSJON (TRANSAKSJON_ID, TRANS_TILSTAND_ID, FIL_INFO_ID, K_TRANSAKSJON_S, PERSON_ID, K_BELOP_T, K_ART, K_ANVISER, FNR_FK, UTBETALES_TIL, OS_ID_FK, " +
                        "OS_LINJE_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, DATO_PERSON_FOM, DATO_REAK_FOM, BELOP, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_EKS_ID_FK, K_TRANS_TOLKNING, " +
                        "SENDT_TIL_OPPDRAG, TREKKVEDTAK_ID_FK, FNR_ENDRET, MOT_ID, OS_STATUS, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, K_TREKKANSVAR, K_TRANS_TILST_T, GRAD) " +
                        "values (99000000, null, 20000816, '00', " + personId + ", '01', 'RNT', 'SPK', '66043800214', null, null, null, '2023-05-01', '2023-05-31', '2023-04-25', '1900-01-01', " +
                        "'1900-01-01', 51700, null, null, '02', '6666666666', 'NY', '0', null, '0', '1', null, '2024-04-24 08:45:08.998930','sokos-spk-mottak', '2024-04-24 08:45:08.999190', " +
                        "'sokos-spk-mottak', 1, '4819', 'ORO', null)",
                ),
            )
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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
    Given("det finnes 2 innTransaksjoner med samme fnr hvor art tilhører ulike fagområder og personen eksisterer ikke i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ny_person_med_2_art_i_ulike_fagomraader.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 2
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes to transaksjoner med tolkning NY") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
                innTransaksjonMap[true]!!.size shouldBe 2
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
    Given("det finnes 2 innTransaksjoner med samme fnr hvor art tilhører samme fagområde og personen eksisterer ikke i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ny_person_med_2_art_i_like_fagomraader.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 2
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en transaksjon med tolkning NY en transaksjon med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
                innTransaksjonMap[true]!!.size shouldBe 2
                innTransaksjonMap[false] shouldBe null

                for (i in innTransaksjonMap[true]!!.indices) {
                    val innTransaksjon = innTransaksjonMap[true]!![i]
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    if (i == 0) {
                        verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY, FNR_IKKE_ENDRET)
                    } else {
                        verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY_EKSIST, FNR_IKKE_ENDRET)
                    }
                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }
    Given("det finnes 4 innTransaksjoner med samme fnr hvor det er 2 ulike art i hvert fagområde og personen eksisterer ikke i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_ny_person_med_4_art_i_2_ulike_fagomraader.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 4
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes to transaksjoner med tolkning NY to transaksjoner med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
                innTransaksjonMap[true]!!.size shouldBe 4
                innTransaksjonMap[false] shouldBe null
                for (i in innTransaksjonMap[true]!!.indices) {
                    val innTransaksjon = innTransaksjonMap[true]!![i]
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    if (i == 0 || i == 2) {
                        verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY, FNR_IKKE_ENDRET)
                    } else {
                        verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY_EKSIST, FNR_IKKE_ENDRET)
                    }
                    val transaksjonTilstand =
                        Db2Listener.transaksjonTilstandRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjonTilstand(transaksjonTilstand, innTransaksjon)
                }
            }
        }
    }
    Given("det finnes 2 innTransaksjoner med samme fnr hvor 1 har art i ikke eksisterende fagområde og personen eksisterer i T_TRANSAKSJON") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_person_med_2_art_og_1_nytt_fagomraade.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 2
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes to transaksjoner, en med tolkning NY og en med tolkning NY_EKSIST") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
                innTransaksjonMap[true]!!.size shouldBe 2
                innTransaksjonMap[false] shouldBe null
                for (i in innTransaksjonMap[true]!!.indices) {
                    val innTransaksjon = innTransaksjonMap[true]!![i]
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    if (i == 0) {
                        verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY, FNR_IKKE_ENDRET)
                    } else {
                        verifyTransaksjon(transaksjon, innTransaksjon, TRANS_TOLKNING_NY_EKSIST, FNR_IKKE_ENDRET)
                    }
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
