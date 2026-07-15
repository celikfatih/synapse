package dev.synapse.application.event;

import dev.synapse.domain.event.DomainEvent;

public record StoredEvent(String id, DomainEvent event, String correlationId, int retryCount) {
}
