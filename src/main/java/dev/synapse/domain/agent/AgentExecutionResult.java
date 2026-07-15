package dev.synapse.domain.agent;

import java.util.Objects;

public record AgentExecutionResult(
        String taskId,
        boolean success,
        String summary,
        String logs
) {
    public AgentExecutionResult {
        Objects.requireNonNull(taskId, "taskId cannot be null");
        Objects.requireNonNull(summary, "summary cannot be null");
        Objects.requireNonNull(logs, "logs cannot be null");
    }

    public static AgentExecutionResult success(String taskId, String summary, String logs) {
        return new AgentExecutionResult(taskId, true, summary, logs);
    }

    public static AgentExecutionResult failure(String taskId, String summary, String logs) {
        return new AgentExecutionResult(taskId, false, summary, logs);
    }
}
