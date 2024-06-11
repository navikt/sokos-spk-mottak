package no.nav.sokos.spk.mottak.service

import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.TestHelper.verifyTransaksjon
import no.nav.sokos.spk.mottak.TestHelper.verifyTransaksjonTilstand
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.config.transaction
import no.nav.sokos.spk.mottak.domain.AvvikTransaksjon
import no.nav.sokos.spk.mottak.domain.BEHANDLET_JA
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_TREKK
import no.nav.sokos.spk.mottak.domain.FNR_IKKE_ENDRET
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY
import no.nav.sokos.spk.mottak.domain.isTransaksjonStatusOk
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import java.sql.SQLException
import java.time.LocalDate

internal class ValidateTransaksjonServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener))

    val validateTransaksjonService = ValidateTransaksjonService(Db2Listener.dataSource)

    Given("det finnes innTransaksjoner som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/person.sql")))
            session.update(queryOf(readFromResource("/database/innTransaksjon.sql")))
            session.update(queryOf(readFromResource("/database/innTransaksjon_avvik.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 15
        When("det valideres") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en ok-transaksjon og en avvikstransaksjon") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOk() }
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
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 2
                innTransaksjonList.forEach { innTransaksjon ->
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
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
        var innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPSTYPE_SKATTEPLIKTIG_UTBETALING

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
        var innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPSTYPE_SKATTEPLIKTIG_UTBETALING

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
        var innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPSTYPE_TREKK

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
        var innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet()
        innTransaksjonList.size shouldBe 1
        innTransaksjonList.first().belopstype shouldBe BELOPSTYPE_TREKK

        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
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
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "11")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som har art hvor påkrevd grad mangler ") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_art_og_manglende_grad.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 16") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "16")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som har negativ grad ") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_negativ_grad.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 16") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "16")
                }
            }
        }
    }

    Given("det finnes en innTransaksjon som har grad større enn 100") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/validering/innTransaksjon_med_grad_over_hundre.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 16") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                innTransaksjonList.forEach { innTransaksjon ->
                    val avvikTransaksjon =
                        Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, "16")
                }
            }
        }
    }

    @Suppress("NAME_SHADOWING")
    Given("det finnes innTransaksjoner som trenger å behandles") {
        Db2Listener.dataSource.transaction { session ->
            session.update(queryOf(readFromResource("/database/person.sql")))
            session.update(queryOf(readFromResource("/database/innTransaksjon.sql")))
        }
        Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 10

        When("det valideres") {
            val dataSourceMock = mockk<HikariDataSource>()
            every { dataSourceMock.connection } throws SQLException("No database connection!")

            val validateTransaksjonService = ValidateTransaksjonService(dataSource = dataSourceMock)
            val exception = shouldThrow<MottakException> { validateTransaksjonService.validateInnTransaksjon() }

            Then("skal det kastet en MottakException med database feil") {
                exception.message shouldBe "Feil under behandling av innTransaksjoner. Feilmelding: No database connection!"
            }
        }
    }
})

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
