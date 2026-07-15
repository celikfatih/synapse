package dev.synapse.adapter.out.agent;

import dev.synapse.application.port.out.AgenticExecutionPort;
import dev.synapse.domain.agent.AgentExecutionRequest;
import dev.synapse.domain.agent.AgentExecutionResult;
import dev.synapse.shared.config.AgentProperties;
import dev.synapse.shared.config.GitProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile({"prod", "default"})
public class DockerAiderExecutionAdapter implements AgenticExecutionPort {

    private final AgentProperties agentProperties;
    private final GitProperties gitProperties;

    @Override
    public AgentExecutionResult execute(AgentExecutionRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("request cannot be null");
        }

        List<String> command = buildDockerCommand(request);
        log.info("Launching Docker sandbox container [{}] for taskId [{}] at host volume [{}]",
                agentProperties.getImage(), request.taskId(), request.workspace().workspacePath());

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true);
        log.debug("Executing command: {}", String.join(" ", processBuilder.command()));

        if (gitProperties != null && gitProperties.getToken() != null && !gitProperties.getToken().isBlank()) {
            processBuilder.environment().put("SYNAPSE_GIT_TOKEN", gitProperties.getToken());
        }

        StringBuilder logOutput = new StringBuilder();
        try {
            Process process = processBuilder.start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    logOutput.append(line).append(System.lineSeparator());
                    log.info("[Docker Sandbox - {}] {}", request.taskId(), line);
                }
            }

            boolean completed = process.waitFor(agentProperties.getTimeoutSeconds(), TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                String errorMsg = "Docker sandbox container timed out after " + agentProperties.getTimeoutSeconds() + " seconds";
                log.error(errorMsg);
                return AgentExecutionResult.failure(request.taskId(), errorMsg, logOutput.toString());
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                String summary = "Docker Aider execution succeeded inside container " + agentProperties.getImage();
                return AgentExecutionResult.success(request.taskId(), summary, logOutput.toString());
            } else {
                String errorMsg = "Docker Aider execution failed with exit code: " + exitCode;
                log.warn(errorMsg);
                return AgentExecutionResult.failure(request.taskId(), errorMsg, logOutput.toString());
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            String errorMsg = "Failed to launch or run Docker sandbox container: " + e.getMessage();
            log.error(errorMsg, e);
            return AgentExecutionResult.failure(request.taskId(), errorMsg, logOutput.toString());
        }
    }

    List<String> buildDockerCommand(AgentExecutionRequest request) {
        List<String> command = new ArrayList<>();
        command.add("docker");
        command.add("run");
        command.add("--rm");
        command.add("--memory=" + agentProperties.getMemoryLimit());
        command.add("--cpus=" + agentProperties.getCpus());
        command.add("-v");
        command.add(request.workspace().workspacePath() + ":" + agentProperties.getContainerWorkspacePath());
        command.add("-w");
        command.add(agentProperties.getContainerWorkspacePath());
        command.add("-e");
        command.add("OLLAMA_API_BASE=" + agentProperties.getApiBaseUrl());
        command.add(agentProperties.getImage());
        command.add("aider");
        command.add("--model");
        command.add("ollama_chat/" + agentProperties.getModel());
        command.add("--yes");
        command.add("--no-stream");
        command.add("--no-auto-commits");
        command.add("--test-cmd");
        command.add(request.testCommand());
        command.add("--message");
        command.add(request.promptMessage());
        return command;
    }
}
