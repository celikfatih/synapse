package dev.synapse.adapter.out.messaging;

import dev.synapse.application.port.out.DomainEventPublisherPort;
import dev.synapse.domain.event.DomainEvent;
import dev.synapse.shared.config.KafkaHeaderNames;
import dev.synapse.shared.config.KafkaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class KafkaDomainEventPublisherAdapter implements DomainEventPublisherPort {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaProperties kafkaProperties;

    @Override
    public void publish(DomainEvent message, String correlationId) {
        Message<DomainEvent> event = buildEvent(message, message.getAggregateId(), correlationId);
        kafkaTemplate.send(event);
    }

    public Message<DomainEvent> buildEvent(DomainEvent message, String aggregateId, String correlationId) {
        return MessageBuilder.withPayload(message)
                .setHeader(KafkaHeaders.TOPIC, kafkaProperties.getTaskTopic())
                .setHeader(KafkaHeaders.KEY, aggregateId)
                .setHeader(KafkaHeaderNames.CORRELATION_ID, correlationId)
                .build();
    }
}
