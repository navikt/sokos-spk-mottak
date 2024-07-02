package no.nav.sokos.spk.mottak.domain

import jakarta.xml.bind.annotation.XmlElement
import java.time.LocalDate

data class AndreTrekk(
    @XmlElement
    var trekkvedtakId: Long? = null,
    @XmlElement
    var debitorOffnr: String,
    @XmlElement
    var trekktypeKode: String,
    @XmlElement
    var trekktypeBeskrivelse: String? = null,
    @XmlElement
    var trekkperiodeFom: LocalDate,
    @XmlElement
    var trekkperiodeTom: LocalDate,
    @XmlElement
    var trekkstatusKode: String? = null,
    @XmlElement
    var trekkstatusBeskrivelse: String? = null,
    @XmlElement
    var kreditorOffnr: String? = null,
    @XmlElement
    var kreditorAvdelingsnr: Int? = null,
    @XmlElement
    var kreditorNavn: String? = null,
    @XmlElement
    var kreditorRef: String,
    @XmlElement
    var kreditorKid: String? = null,
    @XmlElement
    var tssEksternId: String,
    @XmlElement
    var prioritet: String? = null,
    @XmlElement
    var prioritetFom: LocalDate? = null,
    @XmlElement
    var trekkAlternativKode: String,
    @XmlElement
    var trekkAlternativBeskrivelse: String? = null,
    @XmlElement
    var sats: Double,
    @XmlElement
    var belopSaldotrekk: Int? = null,
    @XmlElement
    var belopTrukket: Int? = null,
    @XmlElement
    var datoOppfolging: LocalDate? = null,
    @XmlElement
    var endringsInfo: EndringsInfo,
    @XmlElement
    var fagomradeListe: List<Fagomrade>,
    @XmlElement
    var maksbelop: Int? = null,
    @XmlElement
    var ansvarligEnhet: String? = null,
)
