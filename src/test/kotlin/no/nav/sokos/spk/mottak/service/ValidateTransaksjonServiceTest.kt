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
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPR
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
        When("skal det validere ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes transaksjon og avvik transaksjon") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
                innTransaksjonMap[true]!!.size shouldBe 10
                innTransaksjonMap[false]!!.size shouldBe 5

                innTransaksjonMap[true]!!.forEach { innTransaksjon ->
                    val transaksjon =
                        Db2Listener.transaksjonRepository.getByTransaksjonId(innTransaksjon.innTransaksjonId!!)!!
                    verifyTransaksjon(transaksjon, innTransaksjon)

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

    Given("det finnes to innTransaksjoner som er dubletter og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values  (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null), " +
                            "(20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00000201700', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 201700, 'N', '2024-04-10 09:28:50.816717', 'sokos-spk-mottak', '2024-04-10 09:28:50.816781', 'sokos-spk-mottak', 1, null, null)"
                )
            )
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

    Given("det finnes en innTransaksjon som er dublett med en eksisterende avvist transaksjon og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values  (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_AVV_TRANSAKSJON (AVV_TRANSAKSJON_ID, FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_EKS_ID_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD) " +
                            "values  (1, 20000816, '', '66043800214', '01', 'ETT', 'SPK', null, '2023-06-01', '2023-06-30', '2023-05-25', '00001151600', null, null, '02', '111517616', '2023-06-01', 'sokos-spk-mottak', '2023-05-25', 'sokos-spk-mottak', 1, null)"
                )
            )
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


    Given("det finnes en innTransaksjon som er dublett med en eksisterende transaksjon og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_TRANSAKSJON (TRANSAKSJON_ID, TRANS_TILSTAND_ID, FIL_INFO_ID, K_TRANSAKSJON_S, PERSON_ID, K_BELOP_T, K_ART, K_ANVISER, FNR_FK, UTBETALES_TIL, OS_ID_FK, OS_LINJE_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, DATO_PERSON_FOM, DATO_REAK_FOM, BELOP, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_EKS_ID_FK, K_TRANS_TOLKNING, SENDT_TIL_OPPDRAG, TREKKVEDTAK_ID_FK, FNR_ENDRET, MOT_ID, OS_STATUS, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, K_TREKKANSVAR, K_TRANS_TILST_T, GRAD) " +
                            "values (1, null, 20000816, '00', 9040523, '01', 'BPE', 'SPK', '25100699644', null, null, null, '2023-05-01', '2023-05-31', '2023-04-25', '1900-01-01', '1900-01-01', 51700, null, null, '02', '111517616', 'NY', '0', null, '0', '1', null, '2024-04-24 08:45:08.998930','sokos-spk-mottak', '2024-04-24 08:45:08.999190', 'sokos-spk-mottak', 1, '4819', 'OPR', null)"
                )
            )
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

    Given("det finnes en innTransaksjon som har ugyldig fnr og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800555', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
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

    Given("det finnes en innTransaksjon med beløpstype 01 som har ugyldig DatoFom og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230609', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-09', '2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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

    Given("det finnes en innTransaksjon med beløpstype 01 som har ugyldig DatoTom og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230731', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-07-31', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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

    Given("det finnes en innTransaksjon med beløpstype 03 som har ugyldig DatoFom og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230609', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-09', '2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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

    Given("det finnes en innTransaksjon med beløpstype 03 som har ugyldig DatoTom og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230610', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-10', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
        }
        Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId().size shouldBe 1
        When("det valideres ") {
            validateTransaksjonService.validateInnTransaksjon()
            Then("skal det opprettes en avvikstransaksjon med valideringsfeil 03") {
                val innTransaksjonList = Db2Listener.innTransaksjonRepository.getByBehandletWithPersonId(BEHANDLET_JA)

                val innTransaksjonMap = innTransaksjonList.groupBy { it.isTransaksjonStatusOK() }
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

    Given("det finnes en innTransaksjon som har ugyldig beløpstype og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '04', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
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

    Given("det finnes en innTransaksjon som har ugyldig art og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '01', 'XXX', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
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

    Given("det finnes en innTransaksjon som har ugyldig anviserdato og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230633', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', null, 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
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

    Given("det finnes en innTransaksjon som har ugyldig beløp og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '01', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '0000115160X', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 0, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
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

    Given("det finnes en innTransaksjon som har ugyldig kombinasjon av art og beløpstype og som ikke er behandlet") {
        Db2Listener.dataSource.transaction { session ->
            session.update(
                queryOf(
                    "insert into T_INN_TRANSAKSJON (FIL_INFO_ID, K_TRANSAKSJON_S, FNR_FK, BELOPSTYPE, ART, AVSENDER, UTBETALES_TIL, DATO_FOM_STR, DATO_TOM_STR, DATO_ANVISER_STR, BELOP_STR, REF_TRANS_ID, TEKSTKODE, RECTYPE, TRANS_ID_FK, DATO_FOM, DATO_TOM, DATO_ANVISER, BELOP, BEHANDLET, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON, GRAD, GRAD_STR) " +
                            "values (20000816, null, '66043800214', '03', 'ETT', 'SPK', null, '20230601', '20230630', '20230525', '00001151600', null, null, '02', '111517616', '2023-06-01', '2023-06-30', '2023-05-25', 1151600, 'N', '2024-04-10 09:28:50.816459', 'sokos-spk-mottak', '2024-04-10 09:28:50.816542', 'sokos-spk-mottak', 1, null, null)"
                )
            )
            session.update(
                queryOf(
                    "insert into T_PERSON (FNR_FK, DATO_OPPRETTET, OPPRETTET_AV, DATO_ENDRET, ENDRET_AV, VERSJON) " +
                            "values ('66043800214', '2008-12-01 15:42:58.000000', 'KF37/T_PERSON', '1900-01-01 00:00:00.000000', 'KF37/T_PERSON', 1)"
                )
            )
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

private fun verifyAvvikTransaksjonMedValideringsfeil(
    avvikTransaksjon: AvvikTransaksjon,
    innTransaksjon: InnTransaksjon,
    valideringsfeil: String
) {
    return verifyAvvikTransaksjon(avvikTransaksjon, innTransaksjon, valideringsfeil)
}

private fun verifyAvvikTransaksjon(
    avvikTransaksjon: AvvikTransaksjon,
    innTransaksjon: InnTransaksjon,
    valideringsfeil: String? = null
) {
    val systemId = PropertiesConfig.Configuration().naisAppName

    avvikTransaksjon.avvikTransaksjonId shouldBe innTransaksjon.innTransaksjonId
    avvikTransaksjon.filInfoId shouldBe innTransaksjon.filInfoId
    valideringsfeil?.apply { avvikTransaksjon.transaksjonStatus shouldBe this }
        ?: avvikTransaksjon.transaksjonStatus shouldBe innTransaksjon.transaksjonStatus
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
    transaksjonType: String = TRANSAKSJONSTATUS_OK
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
//    transaksjon.transTolkning shouldBe TRANS_TOLKNING_NY
    transaksjon.sendtTilOppdrag shouldBe "0"
    transaksjon.fnrEndret shouldBe '0'
    transaksjon.motId shouldBe innTransaksjon.innTransaksjonId.toString()
    transaksjon.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
    transaksjon.opprettetAv shouldBe systemId
    transaksjon.datoEndret.toLocalDate() shouldBe LocalDate.now()
    transaksjon.endretAv shouldBe systemId
    transaksjon.versjon shouldBe 1
    transaksjon.transTilstandType shouldBe TRANS_TILSTAND_OPR
    transaksjon.grad shouldBe innTransaksjon.grad
}