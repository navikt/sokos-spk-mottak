package no.nav.sokos.spk.mottak.service

import io.kotest.core.spec.style.BehaviorSpec
import io.mockk.mockk
import no.nav.sokos.spk.mottak.integration.FullmaktClientService
import no.nav.sokos.spk.mottak.listener.Db2Listener

class ValidateTransaksjonServiceTest : BehaviorSpec({
    extensions(listOf(Db2Listener))

    val fullmaktClientService = mockk<FullmaktClientService>()

    val validateTransaksjonService: ValidateTransaksjonService = ValidateTransaksjonService(Db2Listener.dataSource, fullmaktClientService)

    Given("det finnes innTransaksjoner som ikke er behandlet") {

    }
})
