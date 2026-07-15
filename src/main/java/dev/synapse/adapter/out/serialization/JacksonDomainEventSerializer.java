package dev.synapse.adapter.out.serialization;

import dev.synapse.shared.registry.DomainEventRegistry;
import dev.synapse.domain.event.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class JacksonDomainEventSerializer implements DomainEventSerializer {

    private final ObjectMapper objectMapper;
    private final DomainEventRegistry domainEventRegistry;

    @Override
    public String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (Exception e) {
            log.error("Failed to serialize event: {}", event, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public DomainEvent deserialize(String type, String payload) {
        try {
            Class<? extends DomainEvent> clazz = domainEventRegistry.resolve(type);
            return objectMapper.readValue(payload, clazz);
        } catch (Exception e) {
            log.error("Failed to deserialize event: {}", payload, e);
            throw new RuntimeException(e);
        }
    }
}
