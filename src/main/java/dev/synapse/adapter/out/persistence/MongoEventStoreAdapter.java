package dev.synapse.adapter.out.persistence;

import dev.synapse.adapter.out.persistence.entity.DomainEventDocument;
import dev.synapse.adapter.out.persistence.repository.MongoDomainEventCustomRepository;
import dev.synapse.adapter.out.persistence.repository.MongoDomainEventRepository;
import dev.synapse.adapter.out.serialization.DomainEventSerializer;
import dev.synapse.application.event.StoredEvent;
import dev.synapse.application.port.out.LoadDomainEventPort;
import dev.synapse.application.port.out.SaveDomainEventPort;
import dev.synapse.application.port.out.UpdateDomainEventPort;
import dev.synapse.domain.event.DomainEvent;
import dev.synapse.domain.event.EventStatus;
import dev.synapse.domain.event.EventType;
import dev.synapse.domain.event.annotation.DomainEventType;
import dev.synapse.shared.tracing.TracingContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class MongoEventStoreAdapter implements SaveDomainEventPort, LoadDomainEventPort, UpdateDomainEventPort {

    private final MongoDomainEventRepository mongoDomainEventRepository;
    private final MongoDomainEventCustomRepository mongoDomainEventCustomRepository;
    private final DomainEventSerializer domainEventSerializer;

    @Override
    public void save(DomainEvent event) {
        String jsonPayload = domainEventSerializer.serialize(event);
        EventType eventType = getEventType(event);
        DomainEventDocument domainEvent = DomainEventDocument.builder()
                .id(UUID.randomUUID().toString())
                .aggregateId(event.getAggregateId())
                .correlationId(TracingContext.getCorrelationId())
                .eventType(eventType.name())
                .payload(jsonPayload)
                .status(EventStatus.UNPUBLISHED.name())
                .createdAt(event.getCreatedAt())
                .retryCount(0)
                .build();
        mongoDomainEventRepository.save(domainEvent);
    }

    @Override
    public List<StoredEvent> findByStatus(String status) {
        List<DomainEventDocument> events = mongoDomainEventRepository.findAllByStatusOrderByCreatedAtAsc(status);
        return events.stream()
                .map(this::toStoredEvent)
                .collect(Collectors.toList());
    }

    @Override
    public void markAsPublished(String id) {
        mongoDomainEventCustomRepository.markAsPublished(id);
        log.info("Event with id: [{}] marked as published", id);
    }

    @Override
    public void incrementRetryCount(String id) {
        mongoDomainEventCustomRepository.incrementRetryCount(id);
    }

    private StoredEvent toStoredEvent(DomainEventDocument doc) {
        DomainEvent event = domainEventSerializer.deserialize(doc.getEventType(), doc.getPayload());
        return new StoredEvent(doc.getId(), event, doc.getCorrelationId(), doc.getRetryCount());
    }

    private static EventType getEventType(DomainEvent event) {
        DomainEventType annotation = event.getClass().getAnnotation(DomainEventType.class);
        return annotation.value();
    }
}
