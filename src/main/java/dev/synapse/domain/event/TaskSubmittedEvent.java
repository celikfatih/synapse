package dev.synapse.domain.event;

import dev.synapse.domain.event.annotation.DomainEventType;
import dev.synapse.domain.task.Task;

import java.time.Instant;

@DomainEventType(EventType.TASK_SUBMITTED)
public record TaskSubmittedEvent(
        Task.TaskId taskId,
        String message,
        String source,
        Instant createdAt) implements DomainEvent {

    public TaskSubmittedEvent(Task.TaskId taskId, String message, String source) {
        this(taskId, message, source, Instant.now());
    }

    @Override
    public String getAggregateId() {
        return taskId.value();
    }

    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }

    public long getVersion() {
        return -1L;
    }
}
