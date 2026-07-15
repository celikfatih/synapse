package dev.synapse.adapter.in.messaging.interceptor;

import dev.synapse.domain.event.DomainEvent;
import dev.synapse.shared.config.KafkaHeaderNames;
import dev.synapse.shared.tracing.TracingContext;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.kafka.listener.RecordInterceptor;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class CorrelationIdRecordInterceptor implements RecordInterceptor<String, DomainEvent> {

    @Override
    public @Nullable ConsumerRecord<String, DomainEvent> intercept(@NonNull ConsumerRecord<String, DomainEvent> eventRecord,
                                                                   @NonNull Consumer<String, DomainEvent> processor) {
        extractAndSet(eventRecord);
        return eventRecord;
    }

    @Override
    public void afterRecord(@NonNull ConsumerRecord<String, DomainEvent> eventRecord,
                            @NonNull Consumer<String, DomainEvent> consumer) {
        TracingContext.clearCorrelationId();
    }

    public static void extractAndSet(ConsumerRecord<?, ?> eventRecord) {
        Header header = eventRecord.headers().lastHeader(KafkaHeaderNames.CORRELATION_ID);
        String correlationId = (header != null && header.value() != null)
                ? new String(header.value(), StandardCharsets.UTF_8)
                : TracingContext.getCorrelationId();
        TracingContext.setCorrelationId(correlationId);
    }
}
