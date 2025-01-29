package no.nav.sokos.spk.mottak.domain

import java.time.LocalDate

data class AvstemmingInfo(
    val filInfoId: Int,
    val antallOSStatus: Int,
    val antallIkkeOSStatus: Int,
    val datoTransaksjonSendt: LocalDate,
)
