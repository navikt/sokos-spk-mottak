package no.nav.sokos.spk.mottak.mq

import java.util.concurrent.atomic.AtomicBoolean

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

internal class MQListenerCoordinatorTest :
    FunSpec({
        test("flag off closes active listeners once") {
            val toggle = AtomicBoolean(true)
            val listeners = List(3) { FakeManagedMqListener() }
            val coordinator = coordinator(toggle, listeners, listOf(true, true, true))
            coordinator.reconcile()

            toggle.set(false)
            coordinator.reconcile()
            coordinator.reconcile()

            listeners.forEach {
                it.startCount shouldBe 1
                it.closeCount shouldBe 1
            }
        }

        test("environment-disabled listener stays stopped when flag is on") {
            val listeners = List(3) { FakeManagedMqListener() }
            val coordinator = coordinator(AtomicBoolean(true), listeners, listOf(true, false, true))

            coordinator.reconcile()

            listeners.map { it.startCount } shouldBe listOf(1, 0, 1)
        }

        test("flag off on first read starts nothing") {
            val listeners = List(3) { FakeManagedMqListener() }
            val coordinator = coordinator(AtomicBoolean(false), listeners, listOf(true, true, true))

            coordinator.reconcile()

            listeners.map { it.startCount } shouldBe listOf(0, 0, 0)
        }

        test("flag off then on restarts listeners") {
            val toggle = AtomicBoolean(true)
            val listeners = List(3) { FakeManagedMqListener() }
            val coordinator = coordinator(toggle, listeners, listOf(true, true, true))

            coordinator.reconcile()
            toggle.set(false)
            coordinator.reconcile()
            toggle.set(true)
            coordinator.reconcile()
            coordinator.reconcile()

            listeners.map { it.startCount } shouldBe listOf(2, 2, 2)
            listeners.map { it.closeCount } shouldBe listOf(1, 1, 1)
        }

        test("start failure is retried independently of other listeners") {
            val listeners = List(3) { FakeManagedMqListener() }
            listeners[1].startFailure = RuntimeException("start feilet")
            val coordinator = coordinator(AtomicBoolean(true), listeners, listOf(true, true, true))

            coordinator.reconcile()
            listeners.map { it.startCount } shouldBe listOf(1, 0, 1)
            listeners[1].closeCount shouldBe 1

            listeners[1].startFailure = null
            coordinator.reconcile()

            listeners.map { it.startCount } shouldBe listOf(1, 1, 1)
        }

        test("close failure is retried and does not block other listeners") {
            val toggle = AtomicBoolean(true)
            val listeners = List(3) { FakeManagedMqListener() }
            val coordinator = coordinator(toggle, listeners, listOf(true, true, true))
            coordinator.reconcile()
            listeners[0].closeFailure = RuntimeException("close feilet")

            toggle.set(false)
            coordinator.reconcile()
            listeners.map { it.closeCount } shouldBe listOf(0, 1, 1)

            listeners[0].closeFailure = null
            coordinator.reconcile()

            listeners.map { it.closeCount } shouldBe listOf(1, 1, 1)
            listeners.map { it.startCount } shouldBe listOf(1, 1, 1)
        }

        test("create failure is retried") {
            val listener = FakeManagedMqListener()
            var failCreate = true
            val coordinator =
                MQListenerCoordinator(
                    isEnabled = { true },
                    registrations =
                        listOf(
                            ListenerRegistration("listener-0", true) {
                                if (failCreate) throw RuntimeException("create feilet")
                                listener
                            },
                        ),
                )

            coordinator.reconcile()
            listener.startCount shouldBe 0

            failCreate = false
            coordinator.reconcile()
            listener.startCount shouldBe 1
        }

        test("checked exceptions from listener creation are retried") {
            val listener = FakeManagedMqListener()
            var failCreate = true
            val coordinator =
                MQListenerCoordinator(
                    isEnabled = { true },
                    registrations =
                        listOf(
                            ListenerRegistration("listener-0", true) {
                                if (failCreate) throw Exception("create feilet")
                                listener
                            },
                        ),
                )

            coordinator.reconcile()
            listener.startCount shouldBe 0

            failCreate = false
            coordinator.reconcile()
            listener.startCount shouldBe 1
        }

        test("reconcile does not start listeners after coordinator closes") {
            val listeners = List(3) { FakeManagedMqListener() }
            val coordinator = coordinator(AtomicBoolean(true), listeners, listOf(true, true, true))
            coordinator.reconcile()

            coordinator.close()
            coordinator.reconcile()

            listeners.map { it.startCount } shouldBe listOf(1, 1, 1)
            listeners.map { it.closeCount } shouldBe listOf(1, 1, 1)
        }

        test("close closes all active listeners") {
            val listeners = List(3) { FakeManagedMqListener() }
            val coordinator = coordinator(AtomicBoolean(true), listeners, listOf(true, false, true))
            coordinator.reconcile()

            coordinator.close()
            coordinator.close()

            listeners.map { it.closeCount } shouldBe listOf(1, 0, 1)
        }
    })

private class FakeManagedMqListener : ManagedMqListener {
    var startCount = 0
    var closeCount = 0
    var startFailure: RuntimeException? = null
    var closeFailure: RuntimeException? = null

    override fun start() {
        startFailure?.let { throw it }
        startCount++
    }

    override fun close() {
        closeFailure?.let { throw it }
        closeCount++
    }
}

private fun registrations(
    listeners: List<FakeManagedMqListener>,
    allowed: List<Boolean>,
) = listeners.mapIndexed { index, listener ->
    ListenerRegistration("listener-$index", allowed[index]) { listener }
}

private fun coordinator(
    toggle: AtomicBoolean,
    listeners: List<FakeManagedMqListener>,
    allowed: List<Boolean>,
) = MQListenerCoordinator(
    isEnabled = { toggle.get() },
    registrations = registrations(listeners, allowed),
)
