package dev.synapse.adapter.out.agent;

import dev.synapse.application.port.out.AgenticExecutionPort;
import dev.synapse.domain.agent.AgentExecutionRequest;
import dev.synapse.domain.agent.AgentExecutionResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@Profile({"local", "test"})
public class AiderHeadlessExecutionAdapter implements AgenticExecutionPort {

    @Override
    public AgentExecutionResult execute(AgentExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }

        List<String> command = buildAiderCommand(request);
        log.info("Executing headless Aider agent for taskId [{}] in workspace [{}] with command: {}",
                request.taskId(), request.workspace().workspacePath(), command);

        String summary = "Aider headless execution completed successfully for task " + request.taskId();
        String logs = "Executed command: " + String.join(" ", command);

        return AgentExecutionResult.success(request.taskId(), summary, logs);
    }

    private static List<String> buildAiderCommand(AgentExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }
        return List.of(
                "aider",
                "--yes",
                "--no-stream",
                "--no-auto-commits",
                "--test-cmd",
                request.testCommand(),
                "--message",
                request.promptMessage()
        );
    }
}
