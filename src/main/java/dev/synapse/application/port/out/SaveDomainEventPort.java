package dev.synapse.application.port.out;

import dev.synapse.domain.event.DomainEvent;

public interface SaveDomainEventPort {
    /**
     * Saves a domain event to the outbox store.
     * @param event the event to be saved.
     */
    void save(DomainEvent event);
}
