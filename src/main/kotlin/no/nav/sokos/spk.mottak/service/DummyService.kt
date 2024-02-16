package no.nav.sokos.spk.mottak.service

import no.nav.sokos.spk.mottak.domain.DummyDomain

class DummyService {
    fun sayHello(): DummyDomain {
        return DummyDomain("Hello World! Greeting from master branch!")
    }
}