package no.nav.sokos.spk.mottak.metrics

import io.micrometer.core.instrument.Timer
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Gauge

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

    val mqUtbetalingProducerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_utbetaling_mq_producer")
            .help("Counts the number of utbetaling sent to OppdragZ through MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqTrekkProducerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_trekk_mq_producer")
            .help("Counts the number of trekk sent to OppdragZ through MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqUtbetalingListenerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_utbetaling_mq_listener")
            .help("Counts the number of utbetaling receive from OppdragZ through MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqTrekkListenerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_trekk_mq_listener")
            .help("Counts the number of trekk receive from OppdragZ through MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val avstemmingIkkeSendtGauge: Gauge =
        Gauge
            .builder()
            .name("${METRICS_NAMESPACE}_avstemming_ikke_sendt")
            .help("Alerts if avstemming not sent")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val transaksjonValideringsfeilGauge: Gauge =
        Gauge
            .builder()
            .name("${METRICS_NAMESPACE}_transaksjon_valideringsfeil")
            .help("Alerts if transaksjonsvalidering feiler")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val anvisningsfilValideringsfeilGauge: Gauge =
        Gauge
            .builder()
            .name("${METRICS_NAMESPACE}_anvisningsfil_valideringsfeil")
            .help("Alerts if anvisningsfilvalidering feiler")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val returfilGenereringsfeilGauge: Gauge =
        Gauge
            .builder()
            .name("${METRICS_NAMESPACE}_returfil_produksjonsfeil")
            .help("Alerts if returfilgenerering feiler")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val utbetalingsutsendelsesfeilGauge: Gauge =
        Gauge
            .builder()
            .name("${METRICS_NAMESPACE}_utbetaling_utsendelsesfeil")
            .help("Alerts if utbetalingsutsendelse feiler")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val trekkutsendelsesfeilGauge: Gauge =
        Gauge
            .builder()
            .name("${METRICS_NAMESPACE}_trekk_utsendelsesfeil")
            .help("Alerts if trekkutsendelse feiler")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)
}
