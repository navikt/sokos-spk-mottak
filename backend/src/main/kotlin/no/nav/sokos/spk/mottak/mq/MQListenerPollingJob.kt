package no.nav.sokos.spk.mottak.mq

import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

internal class MQListenerPollingJob(
    private val reconcile: () -> Unit,
    private val closeListeners: () -> Unit,
    private val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor(),
) : AutoCloseable {
    private var scheduled: ScheduledFuture<*>? = null
    private var closed = false

    @Synchronized
    fun start() {
        check(!closed) { "MQ-listener polling job is closed" }
        if (scheduled != null) return
        runCheckWithoutCancellingFutureChecks()
        scheduled =
            executor.scheduleWithFixedDelay(
                { runCheckWithoutCancellingFutureChecks() },
                1,
                1,
                TimeUnit.MINUTES,
            )
    }

    private fun runCheckWithoutCancellingFutureChecks() {
        try {
            reconcile()
        } catch (exception: Exception) {
            logger.error(exception) { "Feil ved avstemming av MQ-listenerne" }
        }
    }

    @Synchronized
    override fun close() {
        if (closed) return
        closed = true
        scheduled?.cancel(false)
        executor.shutdown()
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                logger.warn { "MQ-listenerkontrollen kjører fortsatt under nedstenging" }
            }
        } catch (exception: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn(exception) { "Avbrutt venting på MQ-listenerkontrollen under nedstenging" }
        } finally {
            closeListeners()
        }
    }
}
