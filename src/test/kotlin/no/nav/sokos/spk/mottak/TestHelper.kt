package no.nav.sokos.spk.mottak

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import mu.KotlinLogging
import no.nav.sokos.spk.mottak.config.PropertiesConfig
import no.nav.sokos.spk.mottak.domain.FILTYPE_ANVISER
import no.nav.sokos.spk.mottak.domain.FilInfo
import no.nav.sokos.spk.mottak.domain.FilStatus
import no.nav.sokos.spk.mottak.domain.SPK
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.LocalDate
import java.util.stream.Collectors

private val logger = KotlinLogging.logger {}

object TestHelper {
    fun readFromResource(filename: String): String {
        val inputStream = this::class.java.getResourceAsStream(filename)!!
        return BufferedReader(InputStreamReader(inputStream)).lines()
            .parallel().collect(Collectors.joining("\n"))
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
}
