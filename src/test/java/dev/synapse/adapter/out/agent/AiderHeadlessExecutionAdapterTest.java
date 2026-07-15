package dev.synapse.adapter.out.agent;

import dev.synapse.domain.agent.AgentExecutionRequest;
import dev.synapse.domain.agent.AgentExecutionResult;
import dev.synapse.domain.workspace.ProvisionedWorkspace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AiderHeadlessExecutionAdapterTest {

    private AiderHeadlessExecutionAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AiderHeadlessExecutionAdapter();
    }

    @Test
    void execute_ShouldReturnSuccessResult_WhenRequestIsValid() {
        // given
        ProvisionedWorkspace workspace = new ProvisionedWorkspace("PAY-1042", "git@github.com:myorg/repo.git", "/tmp/ws", "feat/PAY-1042");
        AgentExecutionRequest request = new AgentExecutionRequest("PAY-1042", "Fix null refund bug", workspace, "./gradlew test");

        // when
        AgentExecutionResult result = adapter.execute(request);

        // then
        assertThat(result).isNotNull();
        assertThat(result.taskId()).isEqualTo("PAY-1042");
        assertThat(result.success()).isTrue();
        assertThat(result.summary()).contains("Aider headless execution completed");
    }

    @Test
    void execute_ShouldThrowIllegalArgumentException_WhenRequestIsNull() {
        assertThatThrownBy(() -> adapter.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request cannot be null");
    }
}
