package no.nav.sokos.spk.mottak.metrics

import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter
import java.util.concurrent.ConcurrentHashMap

private const val METRICS_NAMESPACE = "sokos_spk_mottak"
const val DATABASE_CALL = "database_call"
const val SERVICE_CALL = "service_call"

object Metrics {
    private val counterCache = ConcurrentHashMap<String, Counter>()
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    fun timer(
        metricName: String,
        className: String,
        method: String,
    ): Timer =
        Timer
            .builder("${METRICS_NAMESPACE}_$metricName")
            .tag("className", className)
            .tag("method", method)
            .description("Timer for database operations")
            .register(prometheusMeterRegistry)

    fun counter(
        metricName: String,
        helpText: String,
    ): Counter =
        counterCache.computeIfAbsent("${METRICS_NAMESPACE}_$metricName") {
            Counter
                .builder()
                .name("${METRICS_NAMESPACE}_$metricName")
                .help(helpText)
                .withoutExemplars()
                .register(prometheusMeterRegistry.prometheusRegistry)
        }

    val mqProducerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_mq_producer")
            .help("Counts the number of messages sent to OppdragZ via MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqConsumerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_mq_consumer")
            .help("Counts the number of messages receive from OppdragZ via MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)
}
