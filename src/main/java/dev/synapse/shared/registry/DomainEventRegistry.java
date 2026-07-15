package dev.synapse.shared.registry;

import dev.synapse.domain.event.DomainEvent;
import dev.synapse.domain.event.annotation.DomainEventType;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
public class DomainEventRegistry {

    private final Map<String, Class<? extends DomainEvent>> registry = new HashMap<>();

    public DomainEventRegistry() {
        initialize();
    }

    public Class<? extends DomainEvent> resolve(String type) {
        return Optional.ofNullable(registry.get(type))
                .orElseThrow(() -> new IllegalArgumentException("Unknown event type: " + type));
    }

    private void initialize() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DomainEventType.class));

        var components = scanner.findCandidateComponents(DomainEvent.class.getPackageName());

        for (BeanDefinition candidate : components) {
            try {
                Class<?> clazz = Class.forName(candidate.getBeanClassName());

                if (!DomainEvent.class.isAssignableFrom(clazz)) {
                    throw new IllegalStateException(clazz.getName() + " must implement DomainEvent");
                }

                DomainEventType annotation = clazz.getAnnotation(DomainEventType.class);

                @SuppressWarnings("unchecked")
                Class<? extends DomainEvent> eventClass = (Class<? extends DomainEvent>) clazz;

                Class<? extends DomainEvent> previous = this.registry.put(annotation.value().name(), eventClass);

                if (previous != null) {
                    throw new IllegalStateException("Duplicate event type: " + annotation.value());
                }
            } catch (ClassCastException | ClassNotFoundException _) {
                throw new IllegalStateException(candidate.getBeanClassName() + " must implement DomainEvent");
            }
        }
    }
}
