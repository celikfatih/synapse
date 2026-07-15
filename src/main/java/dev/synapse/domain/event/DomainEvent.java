package dev.synapse.domain.event;

import java.time.Instant;

/**
 * Base marker and metadata contract for all domain events in Synapse.
 */
public interface DomainEvent {
    String getAggregateId();
    Instant getCreatedAt();
}
