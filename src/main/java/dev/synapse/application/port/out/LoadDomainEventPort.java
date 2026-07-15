package dev.synapse.application.port.out;

import dev.synapse.application.event.StoredEvent;

import java.util.List;

public interface LoadDomainEventPort {
    /**
     * Finds domain events by their status.
     * @param status the status of the event (e.g. UNPUBLISHED).
     * @return a list of domain events with the given status.
     */
    List<StoredEvent> findByStatus(String status);
}
