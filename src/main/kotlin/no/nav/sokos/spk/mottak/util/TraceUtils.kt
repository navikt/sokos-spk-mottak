package no.nav.sokos.spk.mottak.util

import io.opentelemetry.api.trace.Tracer
import org.slf4j.MDC

import no.nav.sokos.spk.mottak.config.openTelemetry

object TraceUtils {
    fun <T> withTracerId(
        tracer: Tracer = openTelemetry.getTracer("no.nav.sokos.spk.mottak"),
        spanName: String = "withTracerId",
        block: () -> T,
    ): T {
        val span = tracer.spanBuilder(spanName).startSpan()
        val context = span.spanContext
        MDC.put("trace_id", context.traceId)
        MDC.put("span_id", context.spanId)
        return try {
            block()
        } finally {
            MDC.remove("trace_id")
            MDC.remove("span_id")
            span.end()
        }
    }
}
