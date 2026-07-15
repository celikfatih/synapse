package dev.synapse.application.event;

import dev.synapse.domain.event.DomainEvent;

public interface DomainEventProcessor<T extends DomainEvent> {
    /**
     * Returns the event class that this processor supports.
     * @return the event class.
     */
    Class<T> eventType();

    /**
     * Processes the given event.
     * @param event the event to be processed.
     */
    void process(T event);
}
