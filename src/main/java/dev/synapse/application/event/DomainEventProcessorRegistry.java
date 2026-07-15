package dev.synapse.application.event;

import dev.synapse.application.port.in.HandleDomainEventUseCase;
import dev.synapse.domain.event.DomainEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class DomainEventProcessorRegistry implements HandleDomainEventUseCase {

    private final Map<Class<? extends DomainEvent>, DomainEventProcessor<?>> processors;

    public DomainEventProcessorRegistry(List<DomainEventProcessor<?>> processors) {
        this.processors = processors.stream()
                .collect(Collectors.toUnmodifiableMap(
                        DomainEventProcessor::eventType,
                        Function.identity()));
    }

    @Override
    public <T extends DomainEvent> void handle(T event) {
        process(event);
    }

    private <T extends DomainEvent> void process(T event) {
        DomainEventProcessor<?> processor = getProcessor(event.getClass());
        invoke(processor, event);
    }

    private DomainEventProcessor<?> getProcessor(Class<? extends DomainEvent> clazz) {
        DomainEventProcessor<?> processor = this.processors.get(clazz);
        if (processor == null) {
            throw new IllegalArgumentException("No processor registered for type: " + clazz.getName());
        }
        return processor;
    }

    private <T extends DomainEvent> void invoke(
            DomainEventProcessor<T> processor, DomainEvent event) {
        processor.process(processor.eventType().cast(event));
    }
}
