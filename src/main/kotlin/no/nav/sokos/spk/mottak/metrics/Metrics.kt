package no.nav.sokos.spk.mottak.metrics

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

private const val METRICS_NAMESPACE = "sokos_spk_mottak"

object Metrics {
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val countFileProcessed: Counter =
        Counter.builder()
            .name("${METRICS_NAMESPACE}_file_processed")
            .help("Counts the number of file processed from SPK")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val countInnTransaksjon: Counter =
        Counter.builder()
            .name("${METRICS_NAMESPACE}_innTransaksjoner")
            .help("Counts the number of innTransaksjoner saved to T_INNTRANSAKSJON table")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqProducerMetricCounter: Counter =
        Counter.builder()
            .name("${METRICS_NAMESPACE}_mq_producer")
            .help("Counts the number of messages sent to OppdragZ via MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqConsumerMetricCounter: Counter =
        Counter.builder()
            .name("${METRICS_NAMESPACE}_mq_consumer")
            .help("Counts the number of messages receive from OppdragZ via MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)
}
