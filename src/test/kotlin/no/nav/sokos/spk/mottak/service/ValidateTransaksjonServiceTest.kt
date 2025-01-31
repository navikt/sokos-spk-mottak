package no.nav.sokos.spk.mottak.service

import java.sql.SQLException
import java.time.LocalDate

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.zaxxer.hikari.HikariDataSource
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import kotliquery.queryOf
import org.apache.http.entity.ContentType.APPLICATION_JSON

import no.nav.pdl.HentIdenterBolk
import no.nav.sokos.spk.mottak.TestData.FNR_LIST
import no.nav.sokos.spk.mottak.TestData.hentIdenterBolkResultMock
import no.nav.sokos.spk.mottak.TestHelper.readFromResource
import no.nav.sokos.spk.mottak.TestHelper.verifyTransaksjon
import no.nav.sokos.spk.mottak.TestHelper.verifyTransaksjonTilstand
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.AvvikTransaksjon
import no.nav.sokos.spk.mottak.domain.BEHANDLET_JA
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_SKATTEPLIKTIG_UTBETALING
import no.nav.sokos.spk.mottak.domain.BELOPSTYPE_TREKK
import no.nav.sokos.spk.mottak.domain.FNR_IKKE_ENDRET
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.TRANS_TOLKNING_NY
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus.ART_MANGLER_GRAD
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus.TRANS_ID_DUBLETT
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus.UGYLDIG_ANVISER_DATO
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus.UGYLDIG_ART
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus.UGYLDIG_BELOP
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus.UGYLDIG_BELOPSTYPE
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus.UGYLDIG_DATO
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus.UGYLDIG_FNR
import no.nav.sokos.spk.mottak.domain.TransaksjonStatus.UGYLDIG_KOMBINASJON_AV_ART_BELOPSTYPE
import no.nav.sokos.spk.mottak.domain.isTransaksjonStatusOk
import no.nav.sokos.spk.mottak.exception.MottakException
import no.nav.sokos.spk.mottak.listener.Db2Listener
import no.nav.sokos.spk.mottak.listener.WiremockListener
import no.nav.sokos.spk.mottak.listener.WiremockListener.wiremock
import no.nav.sokos.spk.mottak.pdl.GraphQLResponse
import no.nav.sokos.spk.mottak.pdl.PdlService
import no.nav.sokos.spk.mottak.util.SQLUtils.transaction

internal class ValidateTransaksjonServiceTest :
    BehaviorSpec({
        extensions(listOf(WiremockListener, Db2Listener))

        val pdlService: PdlService by lazy {
            PdlService(
                pdlUrl = wiremock.baseUrl(),
                accessTokenClient = WiremockListener.accessTokenClient,
            )
        }

        val validateTransaksjonService: ValidateTransaksjonService by lazy {
            ValidateTransaksjonService(
                dataSource = Db2Listener.dataSource,
                innTransaksjonRepository = Db2Listener.innTransaksjonRepository,
                pdlService = pdlService,
            )
        }

        every { Db2Listener.innTransaksjonRepository.findAllFnrWithoutPersonId() } returns emptyList()

        Given("det finnes innTransaksjoner som ikke er behandlet") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/person.sql")))
                session.update(queryOf(readFromResource("/database/innTransaksjon.sql")))
                session.update(queryOf(readFromResource("/database/innTransaksjon_avvik.sql")))
            }
            Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 15
            When("det valideres") {
                validateTransaksjonService.validateInnTransaksjon()
                Then("skal det opprettes 10 ok-transaksjoner og 5 avvikstransaksjoner") {
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, TRANS_ID_DUBLETT.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, TRANS_ID_DUBLETT.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, TRANS_ID_DUBLETT.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_DATO.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_DATO.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_DATO.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_DATO.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_BELOPSTYPE.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_ART.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_ANVISER_DATO.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_BELOP.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_KOMBINASJON_AV_ART_BELOPSTYPE.code)
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
                Then("skal det opprettes 1 avvikstransaksjon med valideringsfeil 16") {
                    val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA)
                    innTransaksjonList.filter { !it.isTransaksjonStatusOk() }.size shouldBe 1
                    innTransaksjonList.forEach { innTransaksjon ->
                        val avvikTransaksjon =
                            Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, ART_MANGLER_GRAD.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, ART_MANGLER_GRAD.code)
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
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, ART_MANGLER_GRAD.code)
                    }
                }
            }
        }

        Given("det finnes innTransaksjoner som trenger å behandles") {
            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/person.sql")))
                session.update(queryOf(readFromResource("/database/innTransaksjon.sql")))
            }
            Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 10

            When("det valideres") {
                val dataSourceMock = mockk<HikariDataSource>()
                every { dataSourceMock.connection } throws SQLException("No database connection!")

                val validateTransaksjonServiceMock =
                    ValidateTransaksjonService(
                        dataSource = dataSourceMock,
                        pdlService = pdlService,
                    )
                val exception = shouldThrow<MottakException> { validateTransaksjonServiceMock.validateInnTransaksjon() }

                Then("skal det kastet en MottakException med databasefeil") {
                    exception.message shouldBe "Feil under behandling av inntransaksjoner. Feilmelding: No database connection!"
                }
            }
        }

        Given("det finnes innTranskasjoner som mangler personId") {

            Db2Listener.dataSource.transaction { session ->
                session.update(queryOf(readFromResource("/database/validate_innTransaksjon_and_person.sql")))
            }
            Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 10
            Db2Listener.personRepository.findByFnr(FNR_LIST).size shouldBe 9

            When("det valideres") {
                every { Db2Listener.innTransaksjonRepository.findAllFnrWithoutPersonId() } answers { callOriginal() }

                val response =
                    GraphQLResponse(
                        data =
                            HentIdenterBolk.Result(
                                listOf(
                                    hentIdenterBolkResultMock(fnr = "43084200248"),
                                    hentIdenterBolkResultMock(fnr = "58052700262"),
                                ),
                            ),
                    )

                wiremock.stubFor(
                    WireMock
                        .post(urlEqualTo("/graphql"))
                        .willReturn(
                            aResponse()
                                .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                                .withStatus(HttpStatusCode.OK.value)
                                .withBody(Json.encodeToString(response)),
                        ),
                )

                Then("skal det opprettes 2 nye personer og 10 ok-transaksjoner blir validert") {
                    validateTransaksjonService.validateInnTransaksjon()
                    Db2Listener.personRepository.findByFnr(FNR_LIST).size shouldBe 11
                    Db2Listener.innTransaksjonRepository
                        .getByBehandlet(BEHANDLET_JA)
                        .filter { it.isTransaksjonStatusOk() }
                        .size shouldBe 10
                }
            }
        }

        Given("det finnes innTranskasjoner som mangler personId og fødselsnummer som trenger oppdatering") {
            When("det valideres mot PDL som returnere 2 personer") {
                Db2Listener.dataSource.transaction { session ->
                    session.update(queryOf(readFromResource("/database/validate_innTransaksjon_and_person.sql")))
                }
                Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 10
                Db2Listener.personRepository.findByFnr(FNR_LIST).size shouldBe 9

                every { Db2Listener.innTransaksjonRepository.findAllFnrWithoutPersonId() } answers { callOriginal() }

                val response =
                    GraphQLResponse(
                        data =
                            HentIdenterBolk.Result(
                                listOf(
                                    hentIdenterBolkResultMock(fnr = "43084200248"),
                                    hentIdenterBolkResultMock(
                                        fnr = "58052700262",
                                        oldFnr = "43084200249",
                                    ),
                                ),
                            ),
                    )

                wiremock.stubFor(
                    WireMock
                        .post(urlEqualTo("/graphql"))
                        .willReturn(
                            aResponse()
                                .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                                .withStatus(HttpStatusCode.OK.value)
                                .withBody(Json.encodeToString(response)),
                        ),
                )

                Then("skal det opprettes 1 ny person og oppdatere 1 fødselsnummer og 10 ok-transaksjoner blir validert") {
                    validateTransaksjonService.validateInnTransaksjon()
                    Db2Listener.personRepository.findByFnr(FNR_LIST).size shouldBe 10
                    Db2Listener.innTransaksjonRepository
                        .getByBehandlet(BEHANDLET_JA)
                        .filter { it.isTransaksjonStatusOk() }
                        .size shouldBe 10
                }
            }

            When("det valideres mot PDL som returnerer 1 person") {

                Db2Listener.dataSource.transaction { session ->
                    session.update(queryOf(readFromResource("/database/validate_innTransaksjon_and_person.sql")))
                }
                Db2Listener.innTransaksjonRepository.getByBehandlet().size shouldBe 10
                Db2Listener.personRepository.findByFnr(FNR_LIST).size shouldBe 9
                every { Db2Listener.innTransaksjonRepository.findAllFnrWithoutPersonId() } answers { callOriginal() }

                val response =
                    GraphQLResponse(
                        data =
                            HentIdenterBolk.Result(
                                listOf(
                                    hentIdenterBolkResultMock(
                                        fnr = "58052700262",
                                        oldFnr = "43084200250",
                                    ),
                                ),
                            ),
                    )

                wiremock.stubFor(
                    WireMock
                        .post(urlEqualTo("/graphql"))
                        .willReturn(
                            aResponse()
                                .withHeader(HttpHeaders.ContentType, APPLICATION_JSON.mimeType)
                                .withStatus(HttpStatusCode.OK.value)
                                .withBody(Json.encodeToString(response)),
                        ),
                )

                Then("skal ingen personer opprettes eller oppdateres og 9 ok-transaksjoner blir validert") {
                    validateTransaksjonService.validateInnTransaksjon()
                    Db2Listener.personRepository.findByFnr(FNR_LIST).size shouldBe 10

                    val innTransaksjonMap = Db2Listener.innTransaksjonRepository.getByBehandlet(BEHANDLET_JA).groupBy { it.isTransaksjonStatusOk() }
                    innTransaksjonMap[true]!!.size shouldBe 9
                    innTransaksjonMap[false]!!.size shouldBe 1
                    innTransaksjonMap[false]!!.forEach { innTransaksjon ->
                        val avvikTransaksjon =
                            Db2Listener.avvikTransaksjonRepository.getByAvvTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                        verifyAvvikTransaksjonMedValideringsfeil(avvikTransaksjon, innTransaksjon, UGYLDIG_FNR.code)
                    }
                }
            }
        }
    })

private fun verifyAvvikTransaksjonMedValideringsfeil(
    avvikTransaksjon: AvvikTransaksjon,
    innTransaksjon: InnTransaksjon,
    valideringsfeil: String,
) = verifyAvvikTransaksjon(avvikTransaksjon, innTransaksjon, valideringsfeil)

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
