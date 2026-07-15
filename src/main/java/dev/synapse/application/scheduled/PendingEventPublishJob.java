package dev.synapse.application.scheduled;

import dev.synapse.application.event.StoredEvent;
import dev.synapse.application.port.out.DomainEventPublisherPort;
import dev.synapse.application.port.out.LoadDomainEventPort;
import dev.synapse.application.port.out.UpdateDomainEventPort;
import dev.synapse.domain.event.EventStatus;
import dev.synapse.shared.tracing.TracingContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PendingEventPublishJob {

    private final LoadDomainEventPort loadDomainEventPort;
    private final UpdateDomainEventPort updateDomainEventPort;
    private final DomainEventPublisherPort domainEventPublisherPort;
    private final Tracer tracer;

    @Scheduled(cron = "*/30 * * * * *")
    public void publishPendingEvents() {
        List<StoredEvent> events = loadDomainEventPort.findByStatus(EventStatus.UNPUBLISHED.name());
        if (CollectionUtils.isEmpty(events)) {
            log.info("No pending events found.");
            return;
        }
        events.forEach(s -> {
            try (var _ = tracer.createBaggageInScope(TracingContext.CORRELATION_ID_KEY, s.correlationId())) {
                domainEventPublisherPort.publish(s.event(), s.correlationId());
                log.info("Published event: {}", s);
                updateDomainEventPort.markAsPublished(s.id());
            } catch (Exception e) {
                log.error("Failed to publish event: {}", s, e);
                updateDomainEventPort.incrementRetryCount(s.id());
            }
        });
    }
}
