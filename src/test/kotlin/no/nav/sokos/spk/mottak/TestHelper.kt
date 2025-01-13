package no.nav.sokos.spk.mottak

import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.util.stream.Collectors

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import jakarta.xml.bind.JAXBElement
import no.trygdeetaten.skjema.oppdrag.ObjectFactory
import no.trygdeetaten.skjema.oppdrag.Oppdrag

import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.FILTYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.InnTransaksjon
import no.nav.sokos.spk.mottak.domain.SPK
import no.nav.sokos.spk.mottak.domain.TRANSAKSJONSTATUS_OK
import no.nav.sokos.spk.mottak.domain.TRANS_TILSTAND_OPPRETTET
import no.nav.sokos.spk.mottak.domain.Transaksjon
import no.nav.sokos.spk.mottak.domain.TransaksjonTilstand
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdrag110
import no.nav.sokos.spk.mottak.domain.converter.OppdragConverter.oppdragsLinje150

object TestHelper {
    fun readFromResource(filename: String): String {
        val inputStream = this::class.java.getResourceAsStream(filename)!!
        return BufferedReader(InputStreamReader(inputStream))
            .lines()
            .parallel()
            .collect(Collectors.joining("\n"))
    }

    fun verifyFilInfo(
        filInfo: FilInfo?,
        filStatus: FilStatus,
        filTilstandType: String,
        feilTekst: String? = null,
        fileType: String = FILTYPE_ANVISER,
        anviser: String = SPK,
    ) {
        filInfo shouldNotBe null
        filInfo?.let {
            it.filInfoId shouldNotBe null
            it.filStatus shouldBe filStatus.code
            it.anviser shouldBe anviser
            it.filType shouldBe fileType
            it.filTilstandType shouldBe filTilstandType
            it.filNavn shouldNotBe null
            it.lopeNr shouldNotBe null
            it.feilTekst shouldBe feilTekst
            it.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
            it.opprettetAv shouldBe PropertiesConfig.Configuration().naisAppName
            it.datoSendt shouldBe null
            it.datoEndret.toLocalDate() shouldBe LocalDate.now()
            it.endretAv shouldBe PropertiesConfig.Configuration().naisAppName
            it.versjon shouldBe 1
        }
    }

    fun verifyTransaksjon(
        transaksjon: Transaksjon,
        innTransaksjon: InnTransaksjon,
        tolkning: String,
        fnrEndret: String,
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
        transaksjon.transTilstandType shouldBe TRANS_TILSTAND_OPPRETTET
        transaksjon.grad shouldBe innTransaksjon.grad
    }

    fun verifyTransaksjonTilstand(
        transaksjonTilstand: TransaksjonTilstand,
        innTransaksjon: InnTransaksjon,
    ) {
        val systemId = PropertiesConfig.Configuration().naisAppName

        transaksjonTilstand.transaksjonId shouldBe innTransaksjon.innTransaksjonId
        transaksjonTilstand.transaksjonTilstandId shouldNotBe null
        transaksjonTilstand.transaksjonTilstandType shouldBe TRANS_TILSTAND_OPPRETTET
        transaksjonTilstand.datoOpprettet.toLocalDate() shouldBe LocalDate.now()
        transaksjonTilstand.opprettetAv shouldBe systemId
        transaksjonTilstand.datoEndret.toLocalDate() shouldBe LocalDate.now()
        transaksjonTilstand.endretAv shouldBe systemId
        transaksjonTilstand.versjon shouldBe 1
    }

    fun List<Transaksjon>.toUtbetalingsOppdrag(): JAXBElement<Oppdrag> =
        ObjectFactory().createOppdrag(
            Oppdrag().apply {
                oppdrag110 =
                    first().oppdrag110().apply {
                        oppdragsLinje150.addAll(map { it.oppdragsLinje150() })
                    }
            },
        )
}
