package dev.synapse.shared.tracing;

import io.opentelemetry.api.trace.Span;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

import java.util.UUID;

@NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class TracingContext {

    public static final String CORRELATION_ID_KEY = "correlationId";
    public static final String TRACE_ID_KEY = "traceId";

    public static String getCorrelationId() {
        String correlationId = MDC.get(CORRELATION_ID_KEY);
        if (correlationId != null && !correlationId.isBlank()) {
            return correlationId;
        }
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            return traceId;
        }
        try {
            Span currentSpan = Span.current();
            if (currentSpan != null && currentSpan.getSpanContext().isValid()) {
                String otelTraceId = currentSpan.getSpanContext().getTraceId();
                if (otelTraceId != null && !otelTraceId.isBlank()) {
                    MDC.put(CORRELATION_ID_KEY, otelTraceId);
                    return otelTraceId;
                }
            }
        } catch (Exception _) {
            // Fallback if Span context check fails
        }
        String fallbackId = UUID.randomUUID().toString();
        MDC.put(CORRELATION_ID_KEY, fallbackId);
        return fallbackId;
    }

    public static void setCorrelationId(String correlationId) {
        MDC.put(CORRELATION_ID_KEY, correlationId);
    }

    public static void clearCorrelationId() {
        MDC.remove(CORRELATION_ID_KEY);
    }
}
