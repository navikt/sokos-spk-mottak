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

    val trekkTransaksjonerTilOppdragCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_trekk_transaksjoner_til_oppdrag")
            .help("Counts the number of trekk transactions sent to OppdragZ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val utbetalingTransaksjonerTilOppdragCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_utbetaling_transaksjoner_til_oppdrag")
            .help("Counts the number of transactions sent to OppdragZ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val transaksjonGodkjentCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_transaksjon_godkjent")
            .help("Counts the number of file processed from SPK")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val transaksjonAvvistCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_transaksjon_avvist")
            .help("Counts the number of file processed from SPK")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val fileProcessedCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_innlest_fil")
            .help("Counts the number of file processed from SPK")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val innTransaksjonCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_inn_transaksjoner")
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
