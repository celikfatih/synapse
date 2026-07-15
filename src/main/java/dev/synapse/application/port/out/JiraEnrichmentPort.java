package dev.synapse.application.port.out;

import dev.synapse.domain.task.EnrichedTaskContext;

public interface JiraEnrichmentPort {
    EnrichedTaskContext enrichTask(String ticketKey);
}
