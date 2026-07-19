package dev.synapse.domain.agent;

import dev.synapse.domain.workspace.ProvisionedWorkspace;

public record AgentWorkspaceExecution(ProvisionedWorkspace workspace, AgentExecutionResult executionResult) {
}
