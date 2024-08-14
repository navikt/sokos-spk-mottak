package no.nav.sokos.spk.mottak.domain.converter

import io.kotest.core.spec.style.ExpectSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import no.nav.sokos.spk.mottak.TestData
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdrag110
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdragsLinje150
import no.nav.sokos.spk.mottak.util.Utils.toLocalDateString
import no.nav.sokos.spk.mottak.util.Utils.toXMLGregorianCalendar
import no.trygdeetaten.skjema.oppdrag.TfradragTillegg
import java.time.LocalDate

class OppdragConverterTest :
    ExpectSpec({
        context("transaksjoner til oppdrag format") {
            expect("oppdrag format i henhold til kravspesifikasjon") {
                val transaksjon = TestData.transaksjonMock()
                val oppdrag110 =
                    transaksjon.oppdrag110().apply {
                        oppdragsLinje150.add(transaksjon.oppdragsLinje150())
                    }
                oppdrag110 shouldNotBe null
                oppdrag110.kodeEndring shouldBe "UEND"
                oppdrag110.kodeFagomraade shouldBe transaksjon.gyldigKombinasjon!!.fagomrade
                oppdrag110.fagsystemId shouldBe transaksjon.personId.toString()
                oppdrag110.utbetFrekvens shouldBe "MND"
                oppdrag110.stonadId shouldBe transaksjon.datoFom!!.withDayOfMonth(1).toLocalDateString()
                oppdrag110.oppdragGjelderId shouldBe transaksjon.fnr
                oppdrag110.datoOppdragGjelderFom shouldBe LocalDate.of(1900, 1, 1).toXMLGregorianCalendar()
                oppdrag110.saksbehId shouldBe "MOT"

                oppdrag110.avstemming115.kodeKomponent shouldBe "MOT"
                oppdrag110.avstemming115.nokkelAvstemming shouldBe transaksjon.filInfoId.toString()
                oppdrag110.avstemming115.tidspktMelding shouldNotBe null

                val oppdrag150 = oppdrag110.oppdragsLinje150.first()
                oppdrag150.kodeEndringLinje shouldBe "NY"
                oppdrag150.delytelseId shouldBe transaksjon.motId
                oppdrag150.kodeKlassifik shouldBe transaksjon.gyldigKombinasjon!!.osKlassifikasjon
                oppdrag150.datoKlassifikFom shouldBe LocalDate.of(1900, 1, 1).toXMLGregorianCalendar()
                oppdrag150.datoVedtakFom shouldBe transaksjon.datoFom!!.toXMLGregorianCalendar()
                oppdrag150.datoVedtakTom shouldBe transaksjon.datoTom!!.toXMLGregorianCalendar()
                oppdrag150.sats shouldBe (transaksjon.belop / 100).toBigDecimal()
                oppdrag150.fradragTillegg shouldBe TfradragTillegg.T
                oppdrag150.typeSats shouldBe "MND"
                oppdrag150.skyldnerId shouldBe transaksjon.fnr
                oppdrag150.brukKjoreplan = "N"
                oppdrag150.saksbehId shouldBe "MOT"
                oppdrag150.utbetalesTilId shouldBe transaksjon.fnr

                oppdrag150.attestant180.first().attestantId shouldBe "MOT"
                oppdrag150.grad170.first().typeGrad shouldBe "UFOR"
                oppdrag150.grad170.first().grad shouldBe 100.toBigInteger()
            }
        }
    })
