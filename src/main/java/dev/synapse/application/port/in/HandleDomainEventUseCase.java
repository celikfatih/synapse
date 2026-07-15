package dev.synapse.application.port.in;

import dev.synapse.domain.event.DomainEvent;

/**
 * Inbound port interface defining the contract for handling consumed domain events.
 */
public interface HandleDomainEventUseCase {
    /**
     * Handles and routes an inbound domain event.
     * @param event the consumed domain event.
     * @param <T> the type of domain event.
     */
    <T extends DomainEvent> void handle(T event);
}
