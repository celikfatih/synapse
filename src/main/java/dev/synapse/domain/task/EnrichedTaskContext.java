package dev.synapse.domain.task;

import java.util.List;
import java.util.Objects;

public record EnrichedTaskContext(
        String ticketKey,
        String summary,
        String description,
        List<String> acceptanceCriteria,
        String targetRepositoryUrl
) {
    public EnrichedTaskContext {
        Objects.requireNonNull(ticketKey, "ticketKey cannot be null");
        Objects.requireNonNull(summary, "summary cannot be null");
        Objects.requireNonNull(description, "description cannot be null");
        acceptanceCriteria = acceptanceCriteria == null ? List.of() : List.copyOf(acceptanceCriteria);
    }
}
