package no.nav.sokos.spk.mottak.service

import no.nav.sokos.spk.mottak.domain.LeveAttester
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.LeveAttesterRepository

class LeveAttesterService(
    private val leveAttesterRepository: LeveAttesterRepository = LeveAttesterRepository(),
) {
    fun getLeveAttester(datoFom: String): List<LeveAttester> {
        val leveAttesterList = leveAttesterRepository.getLeveAttester(datoFom)
        Metrics.innTransaksjonCounter.inc(leveAttesterList.size.toLong())
        return leveAttesterList
    }
}
