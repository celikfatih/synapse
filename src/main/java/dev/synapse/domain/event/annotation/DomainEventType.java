package dev.synapse.domain.event.annotation;

import dev.synapse.domain.event.EventType;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DomainEventType {
    /**
     * The event type.
     * @return the event type.
     */
    EventType value();
}
