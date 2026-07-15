package dev.synapse.adapter.in.messaging;

import dev.synapse.application.port.in.HandleDomainEventUseCase;
import dev.synapse.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDomainEventConsumerAdapter {

    private final HandleDomainEventUseCase handleDomainEventUseCase;

    @KafkaListener(
            topics = "${synapse.kafka.task-topic}",
            groupId = "tasks-consumer-group",
            containerFactory = "manuelAcknowledgeContainerFactory"
    )
    public void consume(ConsumerRecord<String, DomainEvent> eventRecord, Acknowledgment acknowledgment) {
        log.info("Received message: [{}] with partition=[{}]", eventRecord.value(), eventRecord.partition());
        handleDomainEventUseCase.handle(eventRecord.value());
        log.info("Message: [{}] processed successfully", eventRecord.value());
        acknowledgment.acknowledge();
    }
}
