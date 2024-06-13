package no.nav.sokos.spk.mottak.domain

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import java.time.LocalDate

@JacksonXmlRootElement(localName = "AndreTrekk")
data class AndreTrekk(
    var trekkvedtakId: Long? = null,
    var debitorOffnr: String,
    var trekktypeKode: String,
    var trekktypeBeskrivelse: String? = null,
    var trekkperiodeFom: LocalDate,
    var trekkperiodeTom: LocalDate,
    var trekkstatusKode: String? = null,
    var trekkstatusBeskrivelse: String? = null,
    var kreditorOffnr: String? = null,
    var kreditorAvdelingsnr: Int? = null,
    var kreditorNavn: String? = null,
    var kreditorRef: String,
    var kreditorKid: String? = null,
    var tssEksternId: String,
    var prioritet: String? = null,
    var prioritetFom: LocalDate? = null,
    var trekkAlternativKode: String,
    var trekkAlternativBeskrivelse: String? = null,
    var sats: Double,
    var belopSaldotrekk: Int? = null,
    var belopTrukket: Int? = null,
    var datoOppfolging: LocalDate? = null,
    var endringsInfo: EndringsInfo,
    var fagomradeListe: List<Fagomrade>,
    var maksbelop: Int? = null,
    var ansvarligEnhet: String? = null,
)
