package no.nav.sokos.spk.mottak.metrics

import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter

private const val METRICS_NAMESPACE = "sokos_spk_mottak"
const val DATABASE_CALL = "database_call"
const val SERVICE_CALL = "service_call"

object Metrics {
    val prometheusMeterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)

    val timer: (metricName: String, className: String, method: String) -> Timer = { metricName, className, method ->
        Timer
            .builder("${METRICS_NAMESPACE}_$metricName")
            .tag("className", className)
            .tag("method", method)
            .description("Timer for database operations")
            .register(prometheusMeterRegistry)
    }

    val countTrekkTransaksjonerTilOppdrag: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_trekkTransaksjonerTilOppdrag")
            .help("Counts the number of trekk transactions sent to OppdragZ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val countUtbetalingTransaksjonerTilOppdrag: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_utbetalingTransaksjonerTilOppdrag")
            .help("Counts the number of transactions sent to OppdragZ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val countTransaksjonGodkjent: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_transaksjonGodkjent")
            .help("Counts the number of file processed from SPK")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val countTransaksjonAvvist: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_transaksjonAvvist")
            .help("Counts the number of file processed from SPK")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val countFileProcessed: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_fileProcessed")
            .help("Counts the number of file processed from SPK")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val countInnTransaksjon: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_innTransaksjoner")
            .help("Counts the number of transactions received from SPK")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

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
