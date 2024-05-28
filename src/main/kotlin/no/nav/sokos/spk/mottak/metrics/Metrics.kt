package no.nav.sokos.spk.mottak.metrics

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

private const val METRICS_NAMESPACE = "sokos_spk_mottak"

private const val EXAMPLE_COUNTER = "${METRICS_NAMESPACE}_example_counter"

object Metrics {
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val exampleCounter: Counter =
        Counter.builder()
            .name(EXAMPLE_COUNTER)
            .help("Example counter")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)
}
