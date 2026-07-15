package dev.synapse.domain.agent;

import dev.synapse.domain.workspace.ProvisionedWorkspace;

import java.util.Objects;

public record AgentExecutionRequest(
        String taskId,
        String promptMessage,
        ProvisionedWorkspace workspace,
        String testCommand
) {
    public AgentExecutionRequest {
        Objects.requireNonNull(taskId, "taskId cannot be null");
        Objects.requireNonNull(promptMessage, "promptMessage cannot be null");
        Objects.requireNonNull(workspace, "workspace cannot be null");
        Objects.requireNonNull(testCommand, "testCommand cannot be null");
    }
}
