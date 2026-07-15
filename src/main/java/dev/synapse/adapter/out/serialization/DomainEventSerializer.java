package dev.synapse.adapter.out.serialization;

import dev.synapse.domain.event.DomainEvent;

public interface DomainEventSerializer {
    /**
     * Serializes an event to a string.
     * @param event the event to be serialized.
     * @return the serialized event.
     */
    String serialize(DomainEvent event);

    /**
     * Deserializes a string to an event.
     * @param type the type of the event.
     * @param payload the payload of the event.
     * @return the deserialized event.
     */
    DomainEvent deserialize(String type, String payload);
}
