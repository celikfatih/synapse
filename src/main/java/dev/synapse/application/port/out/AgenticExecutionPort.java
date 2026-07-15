package dev.synapse.application.port.out;

import dev.synapse.domain.agent.AgentExecutionRequest;
import dev.synapse.domain.agent.AgentExecutionResult;

public interface AgenticExecutionPort {
    AgentExecutionResult execute(AgentExecutionRequest request);
}
