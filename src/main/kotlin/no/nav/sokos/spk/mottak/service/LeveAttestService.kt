package no.nav.sokos.spk.mottak.service

import no.nav.sokos.spk.mottak.domain.LeveAttest
import no.nav.sokos.spk.mottak.metrics.Metrics
import no.nav.sokos.spk.mottak.repository.LeveAttestRepository

class LeveAttestService(
    private val leveAttestRepository: LeveAttestRepository = LeveAttestRepository(),
) {
    fun getLeveAttester(datoFom: String): List<LeveAttest> {
        val leveAttestList = leveAttestRepository.getLeveAttester(datoFom)
        Metrics.innTransaksjonCounter.inc(leveAttestList.size.toLong())
        return leveAttestList
    }
}
