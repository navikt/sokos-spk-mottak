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
            .help("Counts the number of trekktransaksjoner sent to OppdragZ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val utbetalingTransaksjonerTilOppdragCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_utbetaling_transaksjoner_til_oppdrag")
            .help("Counts the number of utbetalingstransaksjoner sent to OppdragZ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val transaksjonGodkjentCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_transaksjon_godkjent")
            .help("Counts the number of transaksjoner godkjent")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val transaksjonAvvistCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_transaksjon_avvist")
            .help("Counts the number of transaksjoner avvist")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val fileProcessedCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_innlest_fil")
            .help("Counts the number of innleste filer")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val innTransaksjonCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_inn_transaksjoner")
            .help("Counts the number of inntransaksjoner")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val leveAttestCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_fnr_leveattester")
            .help("Counts the number of fnr leveattester")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val innlesningsreturCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_innlesningsretur")
            .help("Counts the number of innlesningsretur")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val avregningsreturCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_avregningsretur")
            .help("Counts the number of avregningsretur")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqUtbetalingProducerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_utbetaling_mq_producer")
            .help("Counts the number of utbetalinger sent to OppdragZ through MQ")
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

    val mqUtbetalingBOQListenerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_utbetaling_boq_mq_listener")
            .help("Counts the number of utbetaling send to BOQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqTrekkListenerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_trekk_mq_listener")
            .help("Counts the number of trekk receive from OppdragZ through MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqTrekkBOQListenerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_trekk_boq_mq_listener")
            .help("Counts the number of trekk send to BOQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqAvregningListenerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_avregning_mq_listener")
            .help("Counts the number of avregning msgs from UR Z through MQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)

    val mqAvregningBOQListenerMetricCounter: Counter =
        Counter
            .builder()
            .name("${METRICS_NAMESPACE}_avregning_boq_mq_listener")
            .help("Counts the number of avregning send to BOQ")
            .withoutExemplars()
            .register(prometheusMeterRegistry.prometheusRegistry)
}
