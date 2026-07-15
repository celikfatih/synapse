package dev.synapse.domain.task;

import java.util.Objects;
import java.util.UUID;

public class Task {
    private final TaskId id;
    private final Requester requester;
    private final Message message;
    private final Source source;
    private final CorrelationId correlationId;
    private TaskStatus status;

    public record TaskId(String value) {
        public TaskId {
            Objects.requireNonNull(value, "TaskId cannot be null");
        }
        public static TaskId generate() {
            return new TaskId(UUID.randomUUID().toString());
        }

        public String getValue() {
            return value;
        }
    }

    public record Requester(String name) {
        public Requester {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("Requester name cannot be null or blank");
            }
        }
    }

    public record Message(String value) {
        public Message {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Message cannot be null or blank");
            }
        }
    }

    public record Source(String value) {
        public Source {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("Source cannot be null or blank");
            }
        }
    }

    public record CorrelationId(String value) {
        public CorrelationId {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException("CorrelationId cannot be null or blank");
            }
        }
    }

    public Task(String requester, String message, String correlationId, String source) {
        this.id = TaskId.generate();
        this.requester = new Requester(requester);
        this.message = new Message(message);
        this.source = new Source(source);
        this.correlationId = new CorrelationId(correlationId);
        this.status = TaskStatus.ACCEPTED;
    }

    public Task(String id, String requester, String message, String source, String correlationId, String status) {
        this.id = new TaskId(id);
        this.requester = new Requester(requester);
        this.message = new Message(message);
        this.source = new Source(source);
        this.correlationId = new CorrelationId(correlationId);
        this.status = TaskStatus.valueOf(status);
    }

    public TaskId getId() {
        return id;
    }

    public Requester getRequester() {
        return requester;
    }

    public Message getMessage() {
        return message;
    }

    public Source getSource() {
        return source;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public CorrelationId getCorrelationId() {
        return correlationId;
    }

    public void startProcessing() {
        if (this.status == TaskStatus.COMPLETED) {
            throw new IllegalStateException("Cannot transition to PROCESSING from terminal status: " + this.status);
        }
        this.status = TaskStatus.PROCESSING;
    }

    public void complete() {
        if (this.status != TaskStatus.PROCESSING) {
            throw new IllegalStateException("Cannot transition to COMPLETED from status: " + this.status);
        }
        this.status = TaskStatus.COMPLETED;
    }

    public void fail() {
        if (this.status == TaskStatus.COMPLETED || this.status == TaskStatus.FAILED) {
            throw new IllegalStateException("Cannot transition to FAILED from terminal status: " + this.status);
        }
        this.status = TaskStatus.FAILED;
    }

    @Override
    public String toString() {
        return "Task{" +
                "id=" + id +
                ", requester=" + requester +
                ", message=" + message +
                ", source=" + source +
                ", correlationId=" + correlationId +
                ", status=" + status +
                '}';
    }
}
