package no.nav.sokos.spk.mottak.util

import io.ktor.http.HttpHeaders
import org.slf4j.MDC
import java.util.UUID

object CallIdUtils {
    fun withCallId(block: () -> Unit) {
        val callId = MDC.get(HttpHeaders.XCorrelationId) ?: generateCallId()
        MDC.put(HttpHeaders.XCorrelationId, callId)
        try {
            block()
        } finally {
            MDC.remove(HttpHeaders.XCorrelationId)
        }
    }

    private fun generateCallId(): String {
        return UUID.randomUUID().toString()
    }
}
