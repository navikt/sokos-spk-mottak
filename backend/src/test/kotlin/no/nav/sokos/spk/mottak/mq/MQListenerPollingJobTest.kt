package no.nav.sokos.spk.mottak.mq

import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify

internal class MQListenerPollingJobTest :
    FunSpec({
        test("start checks immediately and schedules a one-minute fixed delay") {
            val executor = mockk<ScheduledExecutorService>(relaxed = true)
            val check = mockk<() -> Unit>(relaxed = true)
            val closeListeners = mockk<() -> Unit>(relaxed = true)
            every {
                executor.scheduleWithFixedDelay(any<Runnable>(), 1, 1, TimeUnit.MINUTES)
            } returns mockk<ScheduledFuture<*>>(relaxed = true)
            val job = MQListenerPollingJob(check, closeListeners, executor)

            job.start()

            verify(exactly = 1) { check.invoke() }
            verify(exactly = 1) { executor.scheduleWithFixedDelay(any<Runnable>(), 1, 1, TimeUnit.MINUTES) }
        }

        test("scheduled check logs a failure and later checks still run") {
            val executor = mockk<ScheduledExecutorService>(relaxed = true)
            val runnable = slot<Runnable>()
            every {
                executor.scheduleWithFixedDelay(capture(runnable), 1, 1, TimeUnit.MINUTES)
            } returns mockk<ScheduledFuture<*>>(relaxed = true)
            var calls = 0
            val job =
                MQListenerPollingJob(
                    reconcile = {
                        calls++
                        if (calls == 2) throw Exception("feil")
                    },
                    closeListeners = {},
                    executor = executor,
                )

            job.start()
            runnable.captured.run()
            runnable.captured.run()

            calls shouldBe 3
        }

        test("close waits for an active check without interrupting it") {
            val executor = mockk<ScheduledExecutorService>(relaxed = true)
            val closeListeners = mockk<() -> Unit>(relaxed = true)
            every {
                executor.scheduleWithFixedDelay(any<Runnable>(), 1, 1, TimeUnit.MINUTES)
            } returns mockk<ScheduledFuture<*>>(relaxed = true)
            every { executor.awaitTermination(5, TimeUnit.SECONDS) } returns true
            val job = MQListenerPollingJob({}, closeListeners, executor)
            job.start()

            job.close()

            verify(exactly = 1) { executor.shutdown() }
            verify(exactly = 1) { executor.awaitTermination(5, TimeUnit.SECONDS) }
            verify(exactly = 0) { executor.shutdownNow() }
            verify(exactly = 1) { closeListeners.invoke() }
        }

        test("close stops executor and closes listeners once") {
            val executor = mockk<ScheduledExecutorService>(relaxed = true)
            val closeListeners = mockk<() -> Unit>(relaxed = true)
            every {
                executor.scheduleWithFixedDelay(any<Runnable>(), 1, 1, TimeUnit.MINUTES)
            } returns mockk<ScheduledFuture<*>>(relaxed = true)
            every { executor.awaitTermination(5, TimeUnit.SECONDS) } returns true
            val job = MQListenerPollingJob({}, closeListeners, executor)
            job.start()

            job.close()
            job.close()

            verify(exactly = 1) { executor.shutdown() }
            verify(exactly = 1) { executor.awaitTermination(5, TimeUnit.SECONDS) }
            verify(exactly = 0) { executor.shutdownNow() }
            verify(exactly = 1) { closeListeners.invoke() }
        }
    })
