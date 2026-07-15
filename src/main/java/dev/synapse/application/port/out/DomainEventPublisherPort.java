package dev.synapse.application.port.out;

import dev.synapse.domain.event.DomainEvent;

public interface DomainEventPublisherPort {
    /**
     * Publishes an event to a topic.
     * @param message the event message.
     * @param correlationId the correlation ID for the event.
     */
    void publish(DomainEvent message, String correlationId);
}
