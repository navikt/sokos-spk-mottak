import { isSpanContextValid, trace } from "@opentelemetry/api";
import pino, { type DestinationStream, type LoggerOptions } from "pino";

// OpenTelemetry-agenten instrumenterer ikke pino når serveren kjører som ESM,
// så vi henter trace-konteksten selv for å koble logger til traces i Tempo.
const traceContext = () => {
	const spanContext = trace.getActiveSpan()?.spanContext();
	if (!spanContext || !isSpanContextValid(spanContext)) {
		return {};
	}

	return {
		trace_id: spanContext.traceId,
		span_id: spanContext.spanId,
		trace_flags: `0${spanContext.traceFlags.toString(16)}`.slice(-2),
	};
};

const createLogger = (
	defaultConfig: LoggerOptions = {},
	destination?: DestinationStream,
): pino.Logger =>
	pino(
		{
			...defaultConfig,
			mixin: traceContext,
			timestamp: () => `,"@timestamp":"${new Date().toISOString()}"`,
			messageKey: "message",
			formatters: {
				level: (label) => {
					return { level: label.toUpperCase() };
				},
				// biome-ignore lint/suspicious/noExplicitAny: <custom log formatter>
				log: (object: any) => {
					if (object.err) {
						const err = pino.stdSerializers.err(object.err);
						object.stack_trace = err.stack;
						object.type = err.type;
						object.message = err.message;
						delete object.err;
					}

					return object;
				},
			},
		},
		destination,
	);

export const logger = createLogger();
