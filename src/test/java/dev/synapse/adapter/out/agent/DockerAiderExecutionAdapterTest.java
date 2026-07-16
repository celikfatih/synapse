package dev.synapse.adapter.out.agent;

import dev.synapse.domain.agent.AgentExecutionRequest;
import dev.synapse.domain.workspace.ProvisionedWorkspace;
import dev.synapse.shared.config.AgentProperties;
import dev.synapse.shared.config.GitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class DockerAiderExecutionAdapterTest {

    private DockerAiderExecutionAdapter adapter;

    @BeforeEach
    void setUp() {
        AgentProperties agentProperties = new AgentProperties("synapse-sandbox:java25", "/workspace", "8g", "4", 1800L, "qwen2.5-coder:7b", "http://localhost:8080");
        GitProperties gitProperties = new GitProperties("test-git-token", "");
        adapter = new DockerAiderExecutionAdapter(agentProperties, gitProperties);
    }

    @Test
    void execute_ShouldThrowIllegalArgumentException_WhenRequestIsNull() {
        assertThatThrownBy(() -> adapter.execute(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("request cannot be null");
    }

    @Test
    void buildDockerCommand_ShouldConstructCorrectDockerRunCommandWithQuotasAndVolumeMount() {
        // given
        ProvisionedWorkspace workspace = new ProvisionedWorkspace("TASK-555", "git@github.com:myorg/repo.git", "/host/workspaces/TASK-555/workspace", "feat/TASK-555");
        AgentExecutionRequest request = new AgentExecutionRequest("TASK-555", "Fix NPE in service", workspace, "./gradlew test");

        // when
        List<String> command = adapter.buildDockerCommand(request);

        // then
        assertThat(command).containsSequence(
                "docker", "run", "--rm",
                "--memory=8g",
                "--cpus=4",
                "-v", "/host/workspaces/TASK-555/workspace:/workspace",
                "-w", "/workspace",
                "-e", "OLLAMA_API_BASE=http://localhost:8080",
                "synapse-sandbox:java25",
                "aider",
                "--model",
                "ollama_chat/qwen2.5-coder:7b",
                "--yes",
                "--no-stream",
                "--no-auto-commits",
                "--test-cmd", "./gradlew test",
                "--message", "Fix NPE in service"
        );
    }
}
